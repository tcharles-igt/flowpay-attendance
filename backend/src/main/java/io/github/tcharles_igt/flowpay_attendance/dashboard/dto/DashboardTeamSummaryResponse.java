package io.github.tcharles_igt.flowpay_attendance.dashboard.dto;

import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo consolidado de um time operacional.")
public record DashboardTeamSummaryResponse(
	@Schema(description = "Time especializado responsavel pelo atendimento.", example = "CARDS")
	TeamType team,
	@Schema(description = "Casos aguardando distribuicao neste time.", example = "2")
	long waiting,
	@Schema(description = "Casos em andamento neste time.", example = "3")
	long inProgress,
	@Schema(description = "Casos finalizados neste time.", example = "7")
	long finished,
	@Schema(description = "Tempo medio em fila do time, em minutos.", example = "8")
	long averageQueueTimeMinutes,
	@Schema(description = "Tempo medio de atendimento do time, em minutos.", example = "19")
	long averageServiceTimeMinutes
) {
}
