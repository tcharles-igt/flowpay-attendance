package io.github.tcharles_igt.flowpay_attendance.dashboard.dto;

import java.util.List;

public record DashboardResponse(
	long totalAttendances,
	long waiting,
	long inProgress,
	long finished,
	List<DashboardTeamSummaryResponse> teams,
	List<DashboardAttendantResponse> attendants,
	List<DashboardQueueItemResponse> queue
) {
}
