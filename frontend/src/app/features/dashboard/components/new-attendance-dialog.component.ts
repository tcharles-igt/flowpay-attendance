import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { AttendanceSubject, CreateAttendanceRequest, attendanceSubjectOptions } from '../models/dashboard.model';

@Component({
  selector: 'app-new-attendance-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './new-attendance-dialog.component.html',
  styleUrl: './new-attendance-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NewAttendanceDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<NewAttendanceDialogComponent, CreateAttendanceRequest | undefined>);

  protected readonly subjectOptions = attendanceSubjectOptions;
  protected readonly form = this.formBuilder.nonNullable.group({
    customerName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    subject: ['CARD_PROBLEM' as AttendanceSubject, Validators.required]
  });

  protected close(): void {
    this.dialogRef.close();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.dialogRef.close({
      customerName: this.form.controls.customerName.value.trim(),
      subject: this.form.controls.subject.value
    });
  }

  protected hasFieldError(fieldName: 'customerName'): boolean {
    const control = this.form.controls[fieldName];
    return control.invalid && (control.touched || control.dirty);
  }
}
