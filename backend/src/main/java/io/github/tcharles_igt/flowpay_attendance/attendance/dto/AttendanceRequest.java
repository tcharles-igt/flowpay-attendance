package io.github.tcharles_igt.flowpay_attendance.attendance.dto;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para abertura de um novo atendimento.")
public record AttendanceRequest(
	@NotBlank
	@Schema(description = "Nome do cliente que abriu o chamado.", example = "Maria Souza")
	String customerName,
	@NotBlank
	@Size(max = 500)
	@Schema(description = "Mensagem detalhando a solicitacao do cliente.", example = "Meu cartao foi bloqueado apos uma compra internacional.")
	String message,
	@NotNull
	@Schema(description = "Assunto informado pelo cliente; o backend converte isso em time responsavel.", example = "CARD_PROBLEM")
	AttendanceSubject subject
) {
	public AttendanceRequest {
		customerName = customerName != null ? customerName.trim() : null;
		message = message != null ? message.trim() : null;
	}
}
