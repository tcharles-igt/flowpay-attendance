import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';

import { DashboardInProgressAttendance, attendanceSubjectLabels, teamLabels } from '../models/dashboard.model';

@Component({
  selector: 'app-in-progress-table',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './in-progress-table.component.html',
  styleUrl: './in-progress-table.component.scss'
})
export class InProgressTableComponent {
  readonly attendances = input<DashboardInProgressAttendance[]>([]);
  readonly finishingId = input<number | null>(null);
  readonly finish = output<number>();

  protected readonly columns = ['customerName', 'subject', 'team', 'attendant', 'startedAt', 'actions'];
  protected getSubjectLabel(item: DashboardInProgressAttendance): string {
    return attendanceSubjectLabels[item.subject];
  }

  protected getTeamLabel(item: DashboardInProgressAttendance): string {
    return teamLabels[item.team];
  }
}
