import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';

import { DashboardQueueItem, attendanceSubjectLabels, statusLabels, teamLabels } from '../models/dashboard.model';

@Component({
  selector: 'app-queue-table',
  imports: [DatePipe, MatCardModule, MatChipsModule, MatTableModule],
  templateUrl: './queue-table.component.html',
  styleUrl: './queue-table.component.scss'
})
export class QueueTableComponent {
  readonly queue = input<DashboardQueueItem[]>([]);
  readonly referenceTime = input<Date | null>(null);

  protected readonly columns = ['customerName', 'subject', 'team', 'status', 'waitTime', 'createdAt'];
  protected getSubjectLabel(item: DashboardQueueItem): string {
    return attendanceSubjectLabels[item.subject];
  }

  protected getStatusLabel(item: DashboardQueueItem): string {
    return statusLabels[item.status];
  }

  protected getTeamLabel(item: DashboardQueueItem): string {
    return teamLabels[item.team];
  }

  protected getWaitTimeLabel(item: DashboardQueueItem): string {
    const referenceTime = this.referenceTime();

    if (!referenceTime) {
      return '--';
    }

    const waitMinutes = Math.max(0, Math.floor((referenceTime.getTime() - new Date(item.createdAt).getTime()) / 60000));
    return `${waitMinutes} min`;
  }
}
