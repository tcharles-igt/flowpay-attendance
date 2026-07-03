package io.github.tcharles_igt.flowpay_attendance.dashboard.dto;

import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public record DashboardTeamSummaryResponse(
	TeamType team,
	long waiting,
	long inProgress,
	long finished
) {
}
