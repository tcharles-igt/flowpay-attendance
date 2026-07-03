package io.github.tcharles_igt.flowpay_attendance.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.service.DashboardService;
import io.github.tcharles_igt.flowpay_attendance.dashboard.service.DashboardStreamService;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

	@Mock
	private DashboardService dashboardService;

	@Mock
	private DashboardStreamService dashboardStreamService;

	@Test
	void shouldDelegateSnapshotRequestToDashboardService() {
		var response = new DashboardResponse(0, 0, 0, 0, 0, 0, java.util.List.of(), java.util.List.of(), java.util.List.of(),
			java.util.List.of());
		when(dashboardService.getSummary()).thenReturn(response);
		var controller = new DashboardController(dashboardService, dashboardStreamService);

		var result = controller.getSummary();

		assertThat(result).isSameAs(response);
		verify(dashboardService).getSummary();
	}

	@Test
	void shouldDelegateEventStreamConnectionToStreamService() {
		var emitter = new SseEmitter();
		when(dashboardStreamService.connect()).thenReturn(emitter);
		var controller = new DashboardController(dashboardService, dashboardStreamService);

		var result = controller.streamEvents();

		assertThat(result).isSameAs(emitter);
		verify(dashboardStreamService).connect();
	}
}
