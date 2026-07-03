package io.github.tcharles_igt.flowpay_attendance.dashboard.dto;

import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public record DashboardAttendantResponse(
	Long id,
	String name,
	TeamType team,
	long activeAttendances,
	long availableSlots
) {
}
