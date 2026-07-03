import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';

import { DashboardAttendant, TeamType, teamLabels } from '../models/dashboard.model';

@Component({
  selector: 'app-attendants-table',
  imports: [MatCardModule, MatChipsModule, MatTableModule],
  templateUrl: './attendants-table.component.html',
  styleUrl: './attendants-table.component.scss'
})
export class AttendantsTableComponent {
  readonly attendants = input<DashboardAttendant[]>([]);

  protected readonly columns = ['team', 'activeAttendances', 'availableSlots', 'status'];
  protected readonly groupedAttendants = computed(() => {
    const grouped = new Map<TeamType, { team: TeamType; activeAttendances: number; availableSlots: number }>();

    for (const attendant of this.attendants()) {
      const current = grouped.get(attendant.team) ?? {
        team: attendant.team,
        activeAttendances: 0,
        availableSlots: 0
      };

      current.activeAttendances += attendant.activeAttendances;
      current.availableSlots += attendant.availableSlots;
      grouped.set(attendant.team, current);
    }

    return Array.from(grouped.values());
  });

  protected getStatus(attendant: { availableSlots: number }): string {
    return attendant.availableSlots > 0 ? 'Disponivel' : 'Sem vaga';
  }

  protected isAvailable(attendant: { availableSlots: number }): boolean {
    return attendant.availableSlots > 0;
  }

  protected getTeamLabel(attendant: { team: TeamType }): string {
    return teamLabels[attendant.team];
  }
}
