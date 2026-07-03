import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-metric-card',
  imports: [MatCardModule],
  templateUrl: './metric-card.component.html',
  styleUrl: './metric-card.component.scss'
})
export class MetricCardComponent {
  readonly title = input.required<string>();
  readonly value = input.required<string | number>();
  readonly description = input.required<string>();
  readonly tone = input<'neutral' | 'accent' | 'warn'>('neutral');
}
