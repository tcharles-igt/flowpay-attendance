import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatToolbarModule } from '@angular/material/toolbar';
import { EMPTY, Subject, catchError, finalize, merge, switchMap, tap, timer } from 'rxjs';

import { AttendantsTableComponent } from '../components/attendants-table.component';
import { InProgressTableComponent } from '../components/in-progress-table.component';
import { MetricCardComponent } from '../components/metric-card.component';
import { QueueTableComponent } from '../components/queue-table.component';
import { TeamSummaryComponent } from '../components/team-summary.component';
import {
  ApiErrorResponse,
  AttendanceStatus,
  AttendanceSubject,
  CreateAttendanceRequest,
  DashboardStatusFilter,
  DashboardTeamFilter,
  DashboardResponse,
  attendanceSubjectOptions,
  dashboardStatusOptions,
  statusLabels,
  teamLabels
} from '../models/dashboard.model';
import { DashboardApiService } from '../services/dashboard-api.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
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
  private readonly formBuilder = inject(FormBuilder);
  private readonly manualRefresh$ = new Subject<void>();

  protected readonly dashboard = signal<DashboardResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly submitting = signal(false);
  protected readonly finishingId = signal<number | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly syncIssue = signal<string | null>(null);
  protected readonly actionFeedback = signal<{ tone: 'success' | 'error'; title: string; message: string } | null>(null);
  protected readonly lastUpdated = signal<Date | null>(null);
  protected readonly subjectOptions = attendanceSubjectOptions;
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

  protected readonly createAttendanceForm = this.formBuilder.nonNullable.group({
    customerName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    subject: ['CARD_PROBLEM' as AttendanceSubject, Validators.required]
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
    merge(timer(0, 5000), this.manualRefresh$)
      .pipe(
        tap(() => {
          const hasSnapshot = this.dashboard() !== null;
          this.loading.set(!hasSnapshot);
          this.refreshing.set(hasSnapshot);
        }),
        switchMap(() =>
          this.api.getDashboard().pipe(
            tap((dashboard) => {
              this.dashboard.set(dashboard);
              this.errorMessage.set(null);
              this.syncIssue.set(null);
              this.lastUpdated.set(new Date());
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
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  protected submitAttendance(): void {
    if (this.createAttendanceForm.invalid || this.submitting()) {
      this.createAttendanceForm.markAllAsTouched();
      return;
    }

    const payload: CreateAttendanceRequest = {
      customerName: this.createAttendanceForm.controls.customerName.value.trim(),
      subject: this.createAttendanceForm.controls.subject.value
    };

    this.submitting.set(true);

    this.api
      .createAttendance(payload)
      .pipe(
        tap(() => {
          this.actionFeedback.set({
            tone: 'success',
            title: 'Atendimento criado',
            message: 'O novo chamado entrou no fluxo e o painel vai sincronizar o snapshot.'
          });
          this.createAttendanceForm.reset({
            customerName: '',
            subject: 'CARD_PROBLEM'
          });
          this.refreshNow();
        }),
        catchError((error) => {
          this.actionFeedback.set({
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
          this.actionFeedback.set({
            tone: 'success',
            title: 'Atendimento finalizado',
            message: 'A capacidade foi liberada e o dashboard vai buscar a redistribuicao atualizada.'
          });
          this.refreshNow();
        }),
        catchError((error) => {
          this.actionFeedback.set({
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
    this.manualRefresh$.next();
  }

  protected updateTeamFilter(team: DashboardTeamFilter): void {
    this.selectedTeam.set(team);
  }

  protected updateStatusFilter(status: DashboardStatusFilter): void {
    this.selectedStatus.set(status);
  }

  protected clearActionFeedback(): void {
    this.actionFeedback.set(null);
  }

  protected hasFieldError(fieldName: 'customerName'): boolean {
    const control = this.createAttendanceForm.controls[fieldName];
    return control.invalid && (control.touched || control.dirty);
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
