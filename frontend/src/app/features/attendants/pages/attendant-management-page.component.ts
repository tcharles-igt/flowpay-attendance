import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { EMPTY, catchError, finalize, tap } from 'rxjs';

import {
  ConfirmationDialogComponent,
  ConfirmationDialogData
} from '../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ToastComponent } from '../../../shared/components/toast/toast.component';
import { AttendantFormDialogComponent } from '../components/attendant-form-dialog.component';
import {
  ApiErrorResponse,
  AttendantFormValue,
  AttendantResponse,
  AttendantStatusFilter,
  AttendantTeamFilter,
  attendantStatusLabels,
  attendantStatusOptions,
  teamLabels
} from '../models/attendant.model';
import { AttendantApiService } from '../services/attendant-api.service';

@Component({
  selector: 'app-attendant-management-page',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatToolbarModule
  ],
  templateUrl: './attendant-management-page.component.html',
  styleUrl: './attendant-management-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttendantManagementPageComponent {
  private readonly attendantApi = inject(AttendantApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly displayedColumns = [
    'name',
    'team',
    'status',
    'activeAttendances',
    'availableSlots',
    'updatedAt',
    'actions'
  ];
  protected readonly statusOptions = attendantStatusOptions;
  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly submitting = signal(false);
  protected readonly busyRowId = signal<number | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selectedTeam = signal<AttendantTeamFilter>('ALL');
  protected readonly selectedStatus = signal<AttendantStatusFilter>('ALL');
  protected readonly attendants = signal<AttendantResponse[]>([]);

  protected readonly teamOptions = computed<Array<{ value: AttendantTeamFilter; label: string }>>(() => {
    const teams = new Set(this.attendants().map((attendant) => attendant.team));

    return [
      { value: 'ALL', label: 'Todos os times' },
      ...Array.from(teams).map((team) => ({
        value: team,
        label: teamLabels[team]
      }))
    ];
  });

  protected readonly filteredAttendants = computed(() =>
    [...this.attendants()]
      .filter((attendant) => {
        const teamMatches = this.selectedTeam() === 'ALL' || attendant.team === this.selectedTeam();
        const statusMatches =
          this.selectedStatus() === 'ALL' ||
          (this.selectedStatus() === 'ACTIVE' && attendant.active) ||
          (this.selectedStatus() === 'INACTIVE' && !attendant.active);
        return teamMatches && statusMatches;
      })
      .sort((left, right) => left.name.localeCompare(right.name, 'pt-BR'))
  );

  protected readonly summaryCards = computed(() => {
    const snapshot = this.filteredAttendants();
    const activeCount = snapshot.filter((attendant) => attendant.active).length;
    const inactiveCount = snapshot.length - activeCount;
    const availableCapacity = snapshot.reduce((sum, attendant) => sum + attendant.availableSlots, 0);

    return [
      { label: 'Total de atendentes', value: snapshot.length.toString(), tone: 'neutral' },
      { label: 'Ativos', value: activeCount.toString(), tone: 'success' },
      { label: 'Inativos', value: inactiveCount.toString(), tone: 'warn' },
      { label: 'Capacidade disponivel', value: availableCapacity.toString(), tone: 'accent' }
    ];
  });

  constructor() {
    this.loadAttendants();
  }

  protected reload(): void {
    if (this.refreshing()) {
      return;
    }

    this.loadAttendants(true);
  }

  protected updateTeamFilter(team: AttendantTeamFilter): void {
    this.selectedTeam.set(team);
  }

  protected updateStatusFilter(status: AttendantStatusFilter): void {
    this.selectedStatus.set(status);
  }

  protected openCreateDialog(): void {
    if (this.submitting()) {
      return;
    }

    this.dialog
      .open(AttendantFormDialogComponent, {
        width: '30rem',
        maxWidth: 'calc(100vw - 1rem)',
        autoFocus: false,
        data: { mode: 'create' }
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((payload) => {
        if (!payload) {
          return;
        }

        this.createAttendant(payload);
      });
  }

  protected openEditDialog(attendant: AttendantResponse): void {
    if (this.isRowBusy(attendant.id)) {
      return;
    }

    this.dialog
      .open(AttendantFormDialogComponent, {
        width: '30rem',
        maxWidth: 'calc(100vw - 1rem)',
        autoFocus: false,
        data: { mode: 'edit', attendant }
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((payload) => {
        if (!payload) {
          return;
        }

        this.updateAttendant(attendant, payload);
      });
  }

  protected toggleStatus(attendant: AttendantResponse): void {
    if (this.isRowBusy(attendant.id)) {
      return;
    }

    const nextActive = !attendant.active;
    const actionLabel = nextActive ? 'Ativar' : 'Desativar';

    this.openConfirmationDialog(
      {
        title: `${actionLabel} atendente`,
        message: nextActive
          ? `Confirma a reativacao operacional de ${attendant.name}?`
          : `Confirma a desativacao operacional de ${attendant.name}?`,
        confirmLabel: actionLabel
      },
      () => {
        this.busyRowId.set(attendant.id);

        this.attendantApi
          .updateAttendantStatus(attendant.id, { active: nextActive })
          .pipe(
            tap(() => {
              this.showToast(
                'success',
                nextActive ? 'Atendente reativado' : 'Atendente desativado',
                nextActive
                  ? 'O atendente voltou a participar da distribuicao de novos casos.'
                  : 'O atendente deixou de receber novos atendimentos.'
              );
              this.loadAttendants(true);
            }),
            catchError((error) => {
              this.showToast(
                'error',
                `Falha ao ${nextActive ? 'ativar' : 'desativar'} atendente`,
                this.formatError(error)
              );
              return EMPTY;
            }),
            finalize(() => this.busyRowId.set(null)),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe();
      }
    );
  }

  protected isRowBusy(attendantId: number): boolean {
    return this.busyRowId() === attendantId;
  }

  protected getStatusLabel(attendant: AttendantResponse): string {
    return attendant.active ? attendantStatusLabels.ACTIVE : attendantStatusLabels.INACTIVE;
  }

  protected getTeamLabel(team: AttendantResponse['team']): string {
    return teamLabels[team];
  }

  protected getStatusActionLabel(attendant: AttendantResponse): string {
    if (this.isRowBusy(attendant.id)) {
      return 'Salvando...';
    }

    return attendant.active ? 'Desativar' : 'Ativar';
  }

  protected formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  protected trackByAttendantId(_: number, attendant: AttendantResponse): number {
    return attendant.id;
  }

  protected getEmptyMessage(): string {
    if (!this.attendants().length) {
      return 'Nenhum atendente cadastrado ainda. Crie o primeiro para habilitar a distribuicao operacional.';
    }

    return 'Nenhum atendente corresponde aos filtros selecionados.';
  }

  private loadAttendants(isRefresh = false): void {
    this.loading.set(!isRefresh);
    this.refreshing.set(isRefresh);
    this.errorMessage.set(null);

    this.attendantApi
      .getAttendants()
      .pipe(
        tap((attendants) => this.attendants.set(attendants)),
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

  private createAttendant(payload: AttendantFormValue): void {
    this.submitting.set(true);

    this.attendantApi
      .createAttendant({ ...payload, active: true })
      .pipe(
        tap(() => {
          this.showToast(
            'success',
            'Atendente criado',
            'O novo atendente ja entrou na base operacional e a lista foi atualizada.'
          );
          this.loadAttendants(true);
        }),
        catchError((error) => {
          this.showToast('error', 'Falha ao criar atendente', this.formatError(error));
          return EMPTY;
        }),
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private updateAttendant(attendant: AttendantResponse, payload: AttendantFormValue): void {
    this.busyRowId.set(attendant.id);

    this.attendantApi
      .updateAttendant(attendant.id, {
        ...payload,
        active: attendant.active
      })
      .pipe(
        tap(() => {
          this.showToast(
            'success',
            'Atendente atualizado',
            'Os dados operacionais do atendente foram sincronizados com a listagem.'
          );
          this.loadAttendants(true);
        }),
        catchError((error) => {
          this.showToast('error', 'Falha ao atualizar atendente', this.formatError(error));
          return EMPTY;
        }),
        finalize(() => this.busyRowId.set(null)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
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

      if (body?.message && body.message !== 'Invalid request payload') {
        return body.message;
      }

      if (body?.details?.length) {
        return body.details[0];
      }
    }

    return 'Nao foi possivel concluir a operacao.';
  }
}
