package io.github.tcharles_igt.flowpay_attendance.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Leituras operacionais consolidadas para o painel.")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	@Operation(
		summary = "Obter snapshot do dashboard",
		description = "Retorna contadores, metricas de tempo, capacidade dos atendentes e filas operacionais para o frontend."
	)
	public DashboardResponse getSummary() {
		return dashboardService.getSummary();
	}
}
