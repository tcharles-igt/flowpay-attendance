import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { EMPTY, catchError, finalize, forkJoin, tap } from 'rxjs';

import {
  ConfirmationDialogComponent,
  ConfirmationDialogData
} from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ToastComponent } from '../../../shared/components/toast/toast.component';
import { NewAttendanceDialogComponent } from '../../dashboard/components/new-attendance-dialog.component';
import {
  ApiErrorResponse,
  DashboardResponse,
  AttendanceResponse,
  AttendanceStatus,
  CreateAttendanceRequest,
  DashboardStatusFilter,
  DashboardTeamFilter,
  TeamType,
  attendanceSubjectLabels,
  dashboardStatusOptions,
  statusLabels,
  teamLabels
} from '../../dashboard/models/dashboard.model';
import { DashboardApiService } from '../../dashboard/services/dashboard-api.service';
import { AttendanceApiService } from '../services/attendance-api.service';

@Component({
  selector: 'app-attendance-management-page',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatToolbarModule
  ],
  templateUrl: './attendance-management-page.component.html',
  styleUrl: './attendance-management-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttendanceManagementPageComponent {
  private readonly attendanceApi = inject(AttendanceApiService);
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly displayedColumns = ['customerName', 'subject', 'team', 'status', 'createdAt', 'timeline', 'actions'];
  protected readonly pageSizeOptions = [10, 25, 50];
  protected readonly statusOptions = dashboardStatusOptions;
  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly submitting = signal(false);
  protected readonly startingId = signal<number | null>(null);
  protected readonly finishingId = signal<number | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selectedTeam = signal<DashboardTeamFilter>('ALL');
  protected readonly selectedStatus = signal<DashboardStatusFilter>('ALL');
  protected readonly attendances = signal<AttendanceResponse[]>([]);
  protected readonly teamAvailableSlots = signal<Record<TeamType, number>>({
    CARDS: 0,
    LOANS: 0,
    OTHERS: 0
  });
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);

  protected readonly teamOptions = computed<Array<{ value: DashboardTeamFilter; label: string }>>(() => {
    const teams = new Set<TeamType>(this.attendances().map((attendance) => attendance.team));

    return [
      { value: 'ALL', label: 'Todos os times' },
      ...Array.from(teams).map((team) => ({
        value: team,
        label: teamLabels[team]
      }))
    ];
  });

  protected readonly filteredAttendances = computed(() => {
    const selectedTeam = this.selectedTeam();
    const selectedStatus = this.selectedStatus();
    const statusPriority: Record<AttendanceStatus, number> = {
      WAITING: 0,
      IN_PROGRESS: 1,
      FINISHED: 2
    };

    return [...this.attendances().filter((attendance) => {
        const teamMatches = selectedTeam === 'ALL' || attendance.team === selectedTeam;
        const statusMatches = selectedStatus === 'ALL' || attendance.status === selectedStatus;
        return teamMatches && statusMatches;
      })].sort((left: AttendanceResponse, right: AttendanceResponse) => {
        const statusDifference = statusPriority[left.status] - statusPriority[right.status];

        if (statusDifference !== 0) {
          return statusDifference;
        }

        return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
      });
  });

  protected readonly summaryCards = computed(() => {
    const snapshot = this.filteredAttendances();

    return [
      { label: 'Total', value: snapshot.length.toString(), tone: 'neutral' },
      { label: 'Na fila', value: this.countByStatus(snapshot, 'WAITING').toString(), tone: 'warn' },
      { label: 'Em atendimento', value: this.countByStatus(snapshot, 'IN_PROGRESS').toString(), tone: 'accent' },
      { label: 'Finalizados', value: this.countByStatus(snapshot, 'FINISHED').toString(), tone: 'success' }
    ];
  });

  protected readonly paginatedAttendances = computed(() => {
    const start = this.pageIndex() * this.pageSize();
    return this.filteredAttendances().slice(start, start + this.pageSize());
  });

  constructor() {
    effect(() => {
      const total = this.filteredAttendances().length;
      const currentSize = this.pageSize();
      const maxPageIndex = total === 0 ? 0 : Math.floor((total - 1) / currentSize);

      if (this.pageIndex() > maxPageIndex) {
        this.pageIndex.set(maxPageIndex);
      }
    });

    this.loadAttendances();
  }

  protected reload(): void {
    if (this.refreshing()) {
      return;
    }

    this.loadAttendances(true);
  }

  protected updateTeamFilter(team: DashboardTeamFilter): void {
    this.selectedTeam.set(team);
    this.pageIndex.set(0);
  }

  protected updateStatusFilter(status: DashboardStatusFilter): void {
    this.selectedStatus.set(status);
    this.pageIndex.set(0);
  }

  protected updatePagination(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
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

        this.createAttendance(payload);
      });
  }

  protected startAttendance(attendance: AttendanceResponse): void {
    if (this.startingId() === attendance.id || !this.canStartAttendance(attendance)) {
      return;
    }

    this.openConfirmationDialog(
      {
        title: 'Iniciar atendimento',
        message: `Confirma o inicio manual do atendimento de ${attendance.customerName}?`,
        confirmLabel: 'Iniciar atendimento',
        summary: {
          customerName: attendance.customerName,
          subject: this.getSubjectLabel(attendance.subject),
          team: this.getTeamLabel(attendance.team),
          message: attendance.message
        }
      },
      () => {
        this.startingId.set(attendance.id);

        this.attendanceApi
          .startAttendance(attendance.id)
          .pipe(
            tap(() => {
              this.showToast('success', 'Atendimento iniciado', 'O item saiu da fila e entrou em atendimento.');
              this.loadAttendances(true);
            }),
            catchError((error) => {
              this.showToast('error', 'Falha ao iniciar atendimento', this.formatError(error));
              return EMPTY;
            }),
            finalize(() => this.startingId.set(null)),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe();
      }
    );
  }

  protected finishAttendance(attendance: AttendanceResponse): void {
    if (this.finishingId() === attendance.id) {
      return;
    }

    this.openConfirmationDialog(
      {
        title: 'Finalizar atendimento',
        message: `Confirma a finalizacao do atendimento de ${attendance.customerName}?`,
        confirmLabel: 'Finalizar atendimento',
        summary: {
          customerName: attendance.customerName,
          subject: this.getSubjectLabel(attendance.subject),
          team: this.getTeamLabel(attendance.team),
          message: attendance.message
        }
      },
      () => {
        this.finishingId.set(attendance.id);

        this.attendanceApi
          .finishAttendance(attendance.id)
          .pipe(
            tap(() => {
              this.showToast('success', 'Atendimento finalizado', 'A lista foi atualizada com o novo status.');
              this.loadAttendances(true);
            }),
            catchError((error) => {
              this.showToast('error', 'Falha ao finalizar atendimento', this.formatError(error));
              return EMPTY;
            }),
            finalize(() => this.finishingId.set(null)),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe();
      }
    );
  }

  protected canStartAttendance(attendance: AttendanceResponse): boolean {
    return attendance.status === 'WAITING' && this.teamAvailableSlots()[attendance.team] > 0;
  }

  protected canFinish(status: AttendanceStatus): boolean {
    return status === 'IN_PROGRESS';
  }

  protected isRowBusy(attendanceId: number): boolean {
    return this.startingId() === attendanceId || this.finishingId() === attendanceId;
  }

  protected getActionLabel(attendance: AttendanceResponse): string | null {
    if (attendance.status === 'WAITING') {
      return this.startingId() === attendance.id ? 'Iniciando...' : 'Iniciar atendimento';
    }

    if (attendance.status === 'IN_PROGRESS') {
      return this.finishingId() === attendance.id ? 'Finalizando...' : 'Finalizar';
    }

    return null;
  }

  protected handleAttendanceAction(attendance: AttendanceResponse): void {
    if (attendance.status === 'WAITING') {
      this.startAttendance(attendance);
      return;
    }

    if (attendance.status === 'IN_PROGRESS') {
      this.finishAttendance(attendance);
    }
  }

  protected getActionAriaLabel(attendance: AttendanceResponse): string {
    return attendance.status === 'WAITING'
      ? `Iniciar atendimento de ${attendance.customerName}`
      : `Finalizar atendimento de ${attendance.customerName}`;
  }

  protected getSubjectLabel(subject: AttendanceResponse['subject']): string {
    return attendanceSubjectLabels[subject];
  }

  protected getTeamLabel(team: TeamType): string {
    return teamLabels[team];
  }

  protected getStatusLabel(status: AttendanceStatus): string {
    return statusLabels[status];
  }

  protected formatDateTime(value: string | null): string {
    if (!value) {
      return '-';
    }

    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  protected getTimeline(attendance: AttendanceResponse): string {
    if (attendance.finishedAt) {
      return `Encerrado em ${this.formatDateTime(attendance.finishedAt)}`;
    }

    if (attendance.startedAt) {
      return `Iniciado em ${this.formatDateTime(attendance.startedAt)}`;
    }

    return 'Aguardando inicio manual';
  }

  private loadAttendances(isRefresh = false): void {
    this.loading.set(!isRefresh);
    this.refreshing.set(isRefresh);
    this.errorMessage.set(null);

    forkJoin({
      attendances: this.attendanceApi.getAttendances(),
      dashboard: this.dashboardApi.getDashboard()
    })
      .pipe(
        tap(({ attendances, dashboard }) => {
          this.attendances.set(attendances);
          this.teamAvailableSlots.set(this.mapTeamAvailableSlots(dashboard));
        }),
        catchError((error) => {
          this.errorMessage.set(this.formatError(error));
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

  private createAttendance(payload: CreateAttendanceRequest): void {
    this.submitting.set(true);

    this.attendanceApi
      .createAttendance(payload)
      .pipe(
        tap(() => {
          this.showToast('success', 'Atendimento criado', 'O novo chamado entrou na lista de gerenciamento.');
          this.loadAttendances(true);
        }),
        catchError((error) => {
          this.showToast('error', 'Falha ao criar atendimento', this.formatError(error));
          return EMPTY;
        }),
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private countByStatus(attendances: AttendanceResponse[], status: AttendanceStatus): number {
    return attendances.filter((attendance) => attendance.status === status).length;
  }

  private mapTeamAvailableSlots(dashboard: DashboardResponse): Record<TeamType, number> {
    return dashboard.attendants.reduce<Record<TeamType, number>>(
      (slotsByTeam, attendant) => {
        slotsByTeam[attendant.team] += attendant.availableSlots;
        return slotsByTeam;
      },
      {
        CARDS: 0,
        LOANS: 0,
        OTHERS: 0
      }
    );
  }

  private openConfirmationDialog(data: ConfirmationDialogData, onConfirm: () => void): void {
    this.dialog
      .open(ConfirmationDialogComponent, {
        width: '28rem',
        maxWidth: 'calc(100vw - 1rem)',
        autoFocus: false,
        data
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed) => {
        if (confirmed) {
          onConfirm();
        }
      });
  }

  private showToast(tone: 'success' | 'error', title: string, message: string): void {
    this.snackBar.openFromComponent(ToastComponent, {
      duration: 4200,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['app-toast-shell', `app-toast-shell--${tone}`],
      data: { tone, title, message }
    });
  }

  private formatError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as ApiErrorResponse | null;
      return body?.message ?? 'Nao foi possivel concluir a operacao.';
    }

    return 'Nao foi possivel concluir a operacao.';
  }
}
