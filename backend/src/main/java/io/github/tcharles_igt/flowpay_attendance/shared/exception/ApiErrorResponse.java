package io.github.tcharles_igt.flowpay_attendance.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
	OffsetDateTime timestamp,
	int status,
	String error,
	String message,
	List<String> details
) {
}
