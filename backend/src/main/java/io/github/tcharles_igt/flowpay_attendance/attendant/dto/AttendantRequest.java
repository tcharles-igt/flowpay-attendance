package io.github.tcharles_igt.flowpay_attendance.attendant.dto;

import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendantRequest(
	@NotBlank String name,
	@NotNull TeamType team,
	boolean active
) {
}
