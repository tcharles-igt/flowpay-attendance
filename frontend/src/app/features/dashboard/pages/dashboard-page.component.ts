import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatToolbarModule } from '@angular/material/toolbar';
import { EMPTY, catchError, finalize, tap } from 'rxjs';

import { AttendantsTableComponent } from '../components/attendants-table.component';
import { InProgressTableComponent } from '../components/in-progress-table.component';
import { MetricCardComponent } from '../components/metric-card.component';
import { NewAttendanceDialogComponent } from '../components/new-attendance-dialog.component';
import { QueueTableComponent } from '../components/queue-table.component';
import { TeamSummaryComponent } from '../components/team-summary.component';
import {
  ApiErrorResponse,
  AttendanceStatus,
  CreateAttendanceRequest,
  DashboardStatusFilter,
  DashboardTeamFilter,
  DashboardResponse,
  dashboardStatusOptions,
  statusLabels,
  teamLabels
} from '../models/dashboard.model';
import { DashboardApiService } from '../services/dashboard-api.service';
import { ToastComponent } from '../../../shared/components/toast/toast.component';

@Component({
  selector: 'app-dashboard-page',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatToolbarModule,
    AttendantsTableComponent,
    InProgressTableComponent,
    MetricCardComponent,
    QueueTableComponent,
    TeamSummaryComponent
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPageComponent {
  private readonly api = inject(DashboardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private dashboardStreamConnection: { close: () => void } | null = null;
  private initialFallbackRequested = false;
  private streamConnected = false;

  protected readonly dashboard = signal<DashboardResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly submitting = signal(false);
  protected readonly finishingId = signal<number | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly syncIssue = signal<string | null>(null);
  protected readonly lastUpdated = signal<Date | null>(null);
  protected readonly statusOptions = dashboardStatusOptions;
  protected readonly selectedTeam = signal<DashboardTeamFilter>('ALL');
  protected readonly selectedStatus = signal<DashboardStatusFilter>('ALL');

  protected readonly teamOptions = computed(() => {
    const snapshot = this.dashboard();

    if (!snapshot) {
      return [{ value: 'ALL' as const, label: 'Todos os times' }];
    }

    return [
      { value: 'ALL' as const, label: 'Todos os times' },
      ...snapshot.teams.map((team) => ({
        value: team.team,
        label: teamLabels[team.team]
      }))
    ];
  });

  protected readonly filteredDashboard = computed<DashboardResponse | null>(() => {
    const snapshot = this.dashboard();

    if (!snapshot) {
      return null;
    }

    const selectedTeam = this.selectedTeam();
    const selectedStatus = this.selectedStatus();
    const teamMatches = (team: string) => selectedTeam === 'ALL' || team === selectedTeam;
    const statusMatches = (status: AttendanceStatus) => selectedStatus === 'ALL' || status === selectedStatus;

    const teams = snapshot.teams
      .filter((team) => teamMatches(team.team))
      .map((team) => ({
        ...team,
        waiting: selectedStatus === 'ALL' || selectedStatus === 'WAITING' ? team.waiting : 0,
        inProgress: selectedStatus === 'ALL' || selectedStatus === 'IN_PROGRESS' ? team.inProgress : 0,
        finished: selectedStatus === 'ALL' || selectedStatus === 'FINISHED' ? team.finished : 0
      }));

    const attendants = snapshot.attendants.filter((attendant) => teamMatches(attendant.team));
    const queue = snapshot.queue.filter((item) => teamMatches(item.team) && statusMatches(item.status));
    const inProgressAttendances = snapshot.inProgressAttendances.filter(
      (item) => teamMatches(item.team) && statusMatches(item.status)
    );

    return {
      ...snapshot,
      totalAttendances: teams.reduce((sum, team) => sum + team.waiting + team.inProgress + team.finished, 0),
      waiting: teams.reduce((sum, team) => sum + team.waiting, 0),
      inProgress: teams.reduce((sum, team) => sum + team.inProgress, 0),
      finished: teams.reduce((sum, team) => sum + team.finished, 0),
      teams,
      attendants,
      queue,
      inProgressAttendances
    };
  });

  protected readonly filterSummary = computed(() => {
    const team = this.selectedTeam();
    const status = this.selectedStatus();
    const teamLabel = team === 'ALL' ? 'todos os times' : teamLabels[team];
    const statusLabel = status === 'ALL' ? 'todos os status' : statusLabels[status];
    return `Recorte atual: ${teamLabel}, ${statusLabel}.`;
  });

  protected readonly metrics = computed(() => {
    const snapshot = this.filteredDashboard();
    const timingTeams = this.resolveTimingTeams();

    if (!snapshot || !timingTeams.length) {
      return [];
    }

    return [
      {
        title: 'Total monitorado',
        value: snapshot.totalAttendances,
        description: 'Volume consolidado de atendimentos na operacao.',
        tone: 'neutral' as const
      },
      {
        title: 'Fila media',
        value: `${this.getAverageQueueTimeMinutes(timingTeams)} min`,
        description: 'Tempo medio ate a distribuicao ou espera atual do recorte selecionado.',
        tone: 'warn' as const
      },
      {
        title: 'Atendimento medio',
        value: `${this.getAverageServiceTimeMinutes(timingTeams)} min`,
        description: 'Tempo medio consumido nos casos em execucao ou ja encerrados.',
        tone: 'accent' as const
      },
      {
        title: 'Na fila',
        value: snapshot.waiting,
        description: 'Casos aguardando distribuicao para um time.',
        tone: 'warn' as const
      },
      {
        title: 'Em atendimento',
        value: snapshot.inProgress,
        description: 'Chamados ativos consumindo capacidade agora.',
        tone: 'accent' as const
      },
      {
        title: 'Finalizados',
        value: snapshot.finished,
        description: 'Historico ja encerrado no ciclo atual.',
        tone: 'neutral' as const
      }
    ];
  });

  constructor() {
    this.openDashboardStream();
    this.destroyRef.onDestroy(() => this.dashboardStreamConnection?.close());
  }

  protected openNewAttendanceDialog(): void {
    if (this.submitting()) {
      return;
    }

    this.dialog
      .open(NewAttendanceDialogComponent, {
        width: '32rem',
        maxWidth: 'calc(100vw - 1rem)',
        autoFocus: false
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((payload) => {
        if (!payload) {
          return;
        }

        this.submitAttendance(payload);
      });
  }

  protected submitAttendance(payload: CreateAttendanceRequest): void {
    this.submitting.set(true);

    this.api
      .createAttendance(payload)
      .pipe(
        tap(() => {
          this.showToast({
            tone: 'success',
            title: 'Atendimento criado',
            message: 'O novo chamado entrou no fluxo e o painel vai atualizar automaticamente.'
          });
        }),
        catchError((error) => {
          this.showToast({
            tone: 'error',
            title: 'Falha ao abrir atendimento',
            message: this.formatError(error)
          });
          return EMPTY;
        }),
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  protected finishAttendance(attendanceId: number): void {
    if (this.finishingId() === attendanceId) {
      return;
    }

    this.finishingId.set(attendanceId);

    this.api
      .finishAttendance(attendanceId)
      .pipe(
        tap(() => {
          this.showToast({
            tone: 'success',
            title: 'Atendimento finalizado',
            message: 'A capacidade foi liberada e o dashboard vai atualizar automaticamente.'
          });
        }),
        catchError((error) => {
          this.showToast({
            tone: 'error',
            title: 'Falha ao finalizar atendimento',
            message: this.formatError(error)
          });
          return EMPTY;
        }),
        finalize(() => this.finishingId.set(null)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  protected refreshNow(): void {
    this.fetchDashboardSnapshot(true);
  }

  protected updateTeamFilter(team: DashboardTeamFilter): void {
    this.selectedTeam.set(team);
  }

  protected updateStatusFilter(status: DashboardStatusFilter): void {
    this.selectedStatus.set(status);
  }

  private formatError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const apiError = error.error as ApiErrorResponse | null;

      if (apiError?.details?.length) {
        return apiError.details.join(' ');
      }

      if (apiError?.message) {
        return apiError.message;
      }

      if (error.status === 0) {
        return 'Nao foi possivel conectar com a API do backend.';
      }
    }

    return 'Nao foi possivel atualizar o dashboard agora.';
  }

  private showToast(data: { tone: 'success' | 'error'; title: string; message: string }): void {
    this.snackBar.openFromComponent(ToastComponent, {
      data,
      duration: 4500,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['app-toast-shell', `app-toast-shell--${data.tone}`]
    });
  }

  private openDashboardStream(): void {
    this.loading.set(true);
    this.dashboardStreamConnection = this.api.connectDashboardStream({
      onSnapshot: (dashboard) => {
        this.streamConnected = true;
        this.dashboard.set(dashboard);
        this.errorMessage.set(null);
        this.syncIssue.set(null);
        this.lastUpdated.set(new Date());
        this.loading.set(false);
        this.refreshing.set(false);
      },
      onOpen: () => {
        this.streamConnected = true;
        if (this.dashboard()) {
          this.syncIssue.set(null);
        }
      },
      onError: () => {
        this.streamConnected = false;
        const hasSnapshot = this.dashboard() !== null;

        if (hasSnapshot) {
          this.syncIssue.set('Sincronizacao em tempo real indisponivel no momento. Tentando reconectar automaticamente.');
          this.loading.set(false);
          this.refreshing.set(false);
          return;
        }

        if (!this.initialFallbackRequested) {
          this.initialFallbackRequested = true;
          this.fetchDashboardSnapshot(false);
        }
      }
    });
  }

  private fetchDashboardSnapshot(isManualRefresh: boolean): void {
    const hasSnapshot = this.dashboard() !== null;

    this.loading.set(!hasSnapshot);
    this.refreshing.set(hasSnapshot || isManualRefresh);

    this.api
      .getDashboard()
      .pipe(
        tap((dashboard) => {
          this.dashboard.set(dashboard);
          this.errorMessage.set(null);
          this.lastUpdated.set(new Date());

          if (!this.streamConnected) {
            this.syncIssue.set('Exibindo snapshot manual enquanto a sincronizacao em tempo real se recupera.');
          } else {
            this.syncIssue.set(null);
          }
        }),
        catchError((error) => {
          const message = this.formatError(error);

          if (this.dashboard()) {
            this.syncIssue.set(message);
          } else {
            this.errorMessage.set(message);
          }

          return EMPTY;
        }),
        finalize(() => {
          this.loading.set(false);
          this.refreshing.set(false);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private resolveTimingTeams() {
    const snapshot = this.dashboard();
    const selectedTeam = this.selectedTeam();

    if (!snapshot) {
      return [];
    }

    return snapshot.teams.filter((team) => selectedTeam === 'ALL' || team.team === selectedTeam);
  }

  private getAverageQueueTimeMinutes(teams: DashboardResponse['teams']): number {
    const totalAttendances = teams.reduce((sum, team) => sum + team.waiting + team.inProgress + team.finished, 0);

    if (!totalAttendances) {
      return 0;
    }

    const weightedSum = teams.reduce(
      (sum, team) => sum + team.averageQueueTimeMinutes * (team.waiting + team.inProgress + team.finished),
      0
    );

    return Math.round(weightedSum / totalAttendances);
  }

  private getAverageServiceTimeMinutes(teams: DashboardResponse['teams']): number {
    const eligibleAttendances = teams.reduce((sum, team) => sum + team.inProgress + team.finished, 0);

    if (!eligibleAttendances) {
      return 0;
    }

    const weightedSum = teams.reduce(
      (sum, team) => sum + team.averageServiceTimeMinutes * (team.inProgress + team.finished),
      0
    );

    return Math.round(weightedSum / eligibleAttendances);
  }
}
