package io.github.tcharles_igt.flowpay_attendance.attendant.dto;

import jakarta.validation.constraints.NotNull;

public record AttendantStatusRequest(
	@NotNull Boolean active
) {
}
