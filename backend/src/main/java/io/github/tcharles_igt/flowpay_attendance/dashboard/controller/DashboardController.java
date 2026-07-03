package io.github.tcharles_igt.flowpay_attendance.dashboard.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.service.DashboardService;
import io.github.tcharles_igt.flowpay_attendance.dashboard.service.DashboardStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Leituras operacionais consolidadas para o painel.")
public class DashboardController {

	private final DashboardService dashboardService;

	private final DashboardStreamService dashboardStreamService;

	public DashboardController(
		DashboardService dashboardService,
		DashboardStreamService dashboardStreamService
	) {
		this.dashboardService = dashboardService;
		this.dashboardStreamService = dashboardStreamService;
	}

	@GetMapping
	@Operation(
		summary = "Obter snapshot do dashboard",
		description = "Retorna contadores, metricas de tempo, capacidade dos atendentes e filas operacionais para o frontend."
	)
	public DashboardResponse getSummary() {
		return dashboardService.getSummary();
	}

	@GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(
		summary = "Abrir stream SSE do dashboard",
		description = "Entrega snapshot inicial e atualizacoes em tempo real do dashboard operacional."
	)
	public SseEmitter streamEvents() {
		return dashboardStreamService.connect();
	}
}
