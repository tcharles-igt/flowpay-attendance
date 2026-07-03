package io.github.tcharles_igt.flowpay_attendance.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contrato padrao de erro da API.")
public record ApiErrorResponse(
	@Schema(description = "Momento em que o erro foi gerado.", example = "2026-07-03T12:00:00Z")
	OffsetDateTime timestamp,
	@Schema(description = "Codigo HTTP retornado.", example = "422")
	int status,
	@Schema(description = "Descricao curta do status HTTP.", example = "Unprocessable Entity")
	String error,
	@Schema(description = "Mensagem principal do erro.", example = "Only in-progress attendances can be finished")
	String message,
	@Schema(description = "Detalhes adicionais de validacao ou regra de negocio.")
	List<String> details
) {
}
