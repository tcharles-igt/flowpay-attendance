package io.github.tcharles_igt.flowpay_attendance.dashboard.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Visao agregada do dashboard operacional.")
public record DashboardResponse(
	@Schema(description = "Quantidade total de atendimentos monitorados no snapshot.", example = "18")
	long totalAttendances,
	@Schema(description = "Quantidade de atendimentos aguardando capacidade.", example = "4")
	long waiting,
	@Schema(description = "Quantidade de atendimentos em execucao.", example = "9")
	long inProgress,
	@Schema(description = "Quantidade de atendimentos finalizados.", example = "5")
	long finished,
	@Schema(description = "Tempo medio em fila, em minutos, considerando casos aguardando ou ja distribuidos.", example = "12")
	long averageQueueTimeMinutes,
	@Schema(description = "Tempo medio de atendimento, em minutos, considerando casos em andamento ou finalizados.", example = "21")
	long averageServiceTimeMinutes,
	@Schema(description = "Resumo operacional por time.")
	List<DashboardTeamSummaryResponse> teams,
	@Schema(description = "Capacidade atual dos atendentes.")
	List<DashboardAttendantResponse> attendants,
	@Schema(description = "Itens atualmente na fila.")
	List<DashboardQueueItemResponse> queue,
	@Schema(description = "Itens em atendimento neste momento.")
	List<DashboardInProgressAttendanceResponse> inProgressAttendances
) {
}
