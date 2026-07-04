import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';

import { DashboardAttendant, teamLabels } from '../models/dashboard.model';

@Component({
  selector: 'app-attendants-table',
  imports: [MatCardModule, MatChipsModule, MatTableModule],
  templateUrl: './attendants-table.component.html',
  styleUrl: './attendants-table.component.scss'
})
export class AttendantsTableComponent {
  readonly attendants = input<DashboardAttendant[]>([]);

  protected readonly columns = ['name', 'team', 'activeAttendances', 'availableSlots', 'status'];

  protected getStatus(attendant: { availableSlots: number }): string {
    return attendant.availableSlots > 0 ? 'Disponivel' : 'Sem vaga';
  }

  protected isAvailable(attendant: { availableSlots: number }): boolean {
    return attendant.availableSlots > 0;
  }

  protected getTeamLabel(attendant: DashboardAttendant): string {
    return teamLabels[attendant.team];
  }
}
