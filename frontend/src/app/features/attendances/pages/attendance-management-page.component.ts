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
import { EMPTY, catchError, finalize, tap } from 'rxjs';

import { ToastComponent } from '../../../shared/components/toast/toast.component';
import { NewAttendanceDialogComponent } from '../../dashboard/components/new-attendance-dialog.component';
import {
  ApiErrorResponse,
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
  protected readonly finishingId = signal<number | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selectedTeam = signal<DashboardTeamFilter>('ALL');
  protected readonly selectedStatus = signal<DashboardStatusFilter>('ALL');
  protected readonly attendances = signal<AttendanceResponse[]>([]);
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

  protected finishAttendance(attendanceId: number): void {
    if (this.finishingId() === attendanceId) {
      return;
    }

    this.finishingId.set(attendanceId);

    this.dashboardApi
      .finishAttendance(attendanceId)
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

  protected canFinish(status: AttendanceStatus): boolean {
    return status === 'IN_PROGRESS';
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

    return 'Aguardando distribuicao';
  }

  private loadAttendances(isRefresh = false): void {
    this.loading.set(!isRefresh);
    this.refreshing.set(isRefresh);
    this.errorMessage.set(null);

    this.attendanceApi
      .getAttendances()
      .pipe(
        tap((response) => this.attendances.set(response)),
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

    this.dashboardApi
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
