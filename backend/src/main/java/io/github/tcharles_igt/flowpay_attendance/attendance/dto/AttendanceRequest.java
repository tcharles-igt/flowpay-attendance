package io.github.tcharles_igt.flowpay_attendance.attendance.dto;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceRequest(
	@NotBlank String customerName,
	@NotNull AttendanceSubject subject
) {
}
