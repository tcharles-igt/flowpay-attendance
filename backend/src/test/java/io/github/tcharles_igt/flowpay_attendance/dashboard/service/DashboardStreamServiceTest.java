package io.github.tcharles_igt.flowpay_attendance.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardResponse;

@ExtendWith(MockitoExtension.class)
class DashboardStreamServiceTest {

	@Mock
	private DashboardService dashboardService;

	@Mock
	private SseEmitter emitter;

	@Test
	void shouldRemoveBrokenEmitterWithoutCallingCompleteWithError() throws Exception {
		var service = new DashboardStreamService(dashboardService);
		var snapshot = new DashboardResponse(0, 0, 0, 0, 0, 0, java.util.List.of(), java.util.List.of(), java.util.List.of(),
			java.util.List.of());
		when(dashboardService.getSummary()).thenReturn(snapshot);
		doThrow(new IOException("client disconnected")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
		addEmitter(service, "broken-emitter", emitter);

		service.publishDashboardUpdate();

		assertThat(service.activeConnections()).isZero();
		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
		verify(emitter, never()).completeWithError(any());
		service.shutdown();
	}

	@SuppressWarnings("unchecked")
	private void addEmitter(DashboardStreamService service, String emitterId, SseEmitter emitter) throws Exception {
		Field field = DashboardStreamService.class.getDeclaredField("emitters");
		field.setAccessible(true);
		((Map<String, SseEmitter>) field.get(service)).put(emitterId, emitter);
	}
}
