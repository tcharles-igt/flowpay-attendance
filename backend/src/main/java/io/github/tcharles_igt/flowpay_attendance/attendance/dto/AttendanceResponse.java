package io.github.tcharles_igt.flowpay_attendance.attendance.dto;

import java.time.OffsetDateTime;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public record AttendanceResponse(
	Long id,
	String customerName,
	AttendanceSubject subject,
	TeamType team,
	AttendanceStatus status,
	Long attendantId,
	OffsetDateTime createdAt,
	OffsetDateTime startedAt,
	OffsetDateTime finishedAt
) {
}
