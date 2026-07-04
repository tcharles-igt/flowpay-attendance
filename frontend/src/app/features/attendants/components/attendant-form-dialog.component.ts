import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { AttendantFormValue, AttendantResponse, TeamType, teamLabels } from '../models/attendant.model';

export interface AttendantFormDialogData {
  attendant?: AttendantResponse;
  mode: 'create' | 'edit';
}

@Component({
  selector: 'app-attendant-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './attendant-form-dialog.component.html',
  styleUrl: './attendant-form-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttendantFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<AttendantFormDialogComponent, AttendantFormValue | undefined>);

  protected readonly data = inject<AttendantFormDialogData>(MAT_DIALOG_DATA);
  protected readonly teamOptions: Array<{ value: TeamType; label: string }> = (Object.entries(teamLabels) as Array<
    [TeamType, string]
  >).map(([value, label]) => ({ value, label }));
  protected readonly form = this.formBuilder.nonNullable.group({
    name: [this.data.attendant?.name ?? '', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    team: [this.data.attendant?.team ?? ('CARDS' as TeamType), Validators.required]
  });

  protected readonly title = this.data.mode === 'create' ? 'Novo atendente' : 'Editar atendente';
  protected readonly copy =
    this.data.mode === 'create'
      ? 'Cadastre quem pode receber novos atendimentos e ja deixe a operacao pronta para distribuir carga.'
      : 'Atualize o nome exibido e o time responsavel sem sair da visao operacional.';
  protected readonly submitLabel = this.data.mode === 'create' ? 'Criar atendente' : 'Salvar alteracoes';

  protected close(): void {
    this.dialogRef.close();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.dialogRef.close({
      name: this.form.controls.name.value.trim(),
      team: this.form.controls.team.value
    });
  }

  protected hasNameError(): boolean {
    const control = this.form.controls.name;
    return control.invalid && (control.touched || control.dirty);
  }
}
