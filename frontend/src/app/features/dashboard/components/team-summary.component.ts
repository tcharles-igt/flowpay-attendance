import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { DashboardTeamSummary, teamLabels } from '../models/dashboard.model';

@Component({
  selector: 'app-team-summary',
  imports: [MatCardModule, MatProgressBarModule],
  templateUrl: './team-summary.component.html',
  styleUrl: './team-summary.component.scss'
})
export class TeamSummaryComponent {
  readonly teams = input<DashboardTeamSummary[]>([]);

  protected readonly rows = computed(() =>
    this.teams().map((team) => {
      const total = team.waiting + team.inProgress + team.finished;
      const completion = total > 0 ? Math.round((team.finished / total) * 100) : 0;

      return {
        ...team,
        label: teamLabels[team.team],
        total,
        completion
      };
    })
  );
}
