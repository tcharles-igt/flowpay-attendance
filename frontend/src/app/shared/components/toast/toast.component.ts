import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_SNACK_BAR_DATA, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-toast',
  imports: [MatSnackBarModule],
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToastComponent {
  protected readonly data = inject<{ title: string; message: string; tone: 'success' | 'error' }>(MAT_SNACK_BAR_DATA);
}
