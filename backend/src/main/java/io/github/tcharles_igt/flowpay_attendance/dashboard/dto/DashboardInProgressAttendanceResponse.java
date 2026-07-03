package io.github.tcharles_igt.flowpay_attendance.dashboard.dto;

import java.time.OffsetDateTime;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public record DashboardInProgressAttendanceResponse(
	Long id,
	String customerName,
	AttendanceSubject subject,
	TeamType team,
	AttendanceStatus status,
	Long attendantId,
	String attendantName,
	OffsetDateTime startedAt
) {
}
