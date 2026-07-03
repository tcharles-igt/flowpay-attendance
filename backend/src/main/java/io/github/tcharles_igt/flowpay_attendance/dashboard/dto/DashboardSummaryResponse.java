package io.github.tcharles_igt.flowpay_attendance.dashboard.dto;

public record DashboardSummaryResponse(
	long waitingAttendances,
	long inProgressAttendances,
	long finishedAttendances,
	long activeAttendants
) {
}
