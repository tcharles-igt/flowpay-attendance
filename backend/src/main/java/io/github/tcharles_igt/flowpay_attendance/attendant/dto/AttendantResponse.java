package io.github.tcharles_igt.flowpay_attendance.attendant.dto;

import java.time.OffsetDateTime;

import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public record AttendantResponse(
	Long id,
	String name,
	TeamType team,
	boolean active,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {
}
