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
  AttendanceSubject,
  CreateAttendanceRequest,
  DashboardResponse,
  attendanceSubjectOptions
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
  protected readonly lastUpdated = signal<Date | null>(null);
  protected readonly subjectOptions = attendanceSubjectOptions;

  protected readonly createAttendanceForm = this.formBuilder.nonNullable.group({
    customerName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    subject: ['CARD_PROBLEM' as AttendanceSubject, Validators.required]
  });

  protected readonly metrics = computed(() => {
    const snapshot = this.dashboard();

    if (!snapshot) {
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
              this.lastUpdated.set(new Date());
            }),
            catchError((error) => {
              this.errorMessage.set(this.formatError(error));
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
          this.createAttendanceForm.reset({
            customerName: '',
            subject: 'CARD_PROBLEM'
          });
          this.refreshNow();
        }),
        catchError((error) => {
          this.errorMessage.set(this.formatError(error));
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
        tap(() => this.refreshNow()),
        catchError((error) => {
          this.errorMessage.set(this.formatError(error));
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
}
