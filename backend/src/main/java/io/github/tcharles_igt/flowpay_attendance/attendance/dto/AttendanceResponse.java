package io.github.tcharles_igt.flowpay_attendance.attendance.dto;

import java.time.OffsetDateTime;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representacao de um atendimento dentro da operacao.")
public record AttendanceResponse(
	@Schema(example = "42")
	Long id,
	@Schema(example = "Maria Souza")
	String customerName,
	@Schema(example = "CARD_PROBLEM")
	AttendanceSubject subject,
	@Schema(example = "CARDS")
	TeamType team,
	@Schema(example = "IN_PROGRESS")
	AttendanceStatus status,
	@Schema(example = "3")
	Long attendantId,
	@Schema(example = "2026-07-03T11:50:00Z")
	OffsetDateTime createdAt,
	@Schema(example = "2026-07-03T11:51:00Z")
	OffsetDateTime startedAt,
	@Schema(example = "2026-07-03T12:12:00Z")
	OffsetDateTime finishedAt
) {
}
