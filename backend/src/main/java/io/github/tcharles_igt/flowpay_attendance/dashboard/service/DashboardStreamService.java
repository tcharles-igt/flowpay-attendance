package io.github.tcharles_igt.flowpay_attendance.dashboard.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

@Service
public class DashboardStreamService {

	static final String DASHBOARD_UPDATED_EVENT = "dashboard-updated";
	static final String HEARTBEAT_EVENT = "heartbeat";

	private static final long SSE_TIMEOUT_MILLIS = 0L;
	private static final long HEARTBEAT_INTERVAL_SECONDS = 15L;

	private final DashboardService dashboardService;

	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

	public DashboardStreamService(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
		heartbeatExecutor.scheduleAtFixedRate(
			this::sendHeartbeats,
			HEARTBEAT_INTERVAL_SECONDS,
			HEARTBEAT_INTERVAL_SECONDS,
			TimeUnit.SECONDS
		);
	}

	public SseEmitter connect() {
		var emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
		var emitterId = UUID.randomUUID().toString();
		emitters.put(emitterId, emitter);

		emitter.onCompletion(() -> removeEmitter(emitterId));
		emitter.onTimeout(() -> removeEmitter(emitterId));
		emitter.onError(error -> removeEmitter(emitterId));

		sendDashboardSnapshot(emitterId, emitter);
		return emitter;
	}

	public void publishDashboardUpdate() {
		var snapshot = dashboardService.getSummary();
		emitters.forEach((emitterId, emitter) -> sendEvent(
			emitterId,
			emitter,
			SseEmitter.event().name(DASHBOARD_UPDATED_EVENT).data(snapshot)
		));
	}

	@PreDestroy
	void shutdown() {
		heartbeatExecutor.shutdownNow();
	}

	int activeConnections() {
		return emitters.size();
	}

	private void sendHeartbeats() {
		emitters.forEach((emitterId, emitter) -> sendEvent(
			emitterId,
			emitter,
			SseEmitter.event().name(HEARTBEAT_EVENT).data("keepalive")
		));
	}

	private void sendDashboardSnapshot(String emitterId, SseEmitter emitter) {
		sendEvent(
			emitterId,
			emitter,
			SseEmitter.event().name(DASHBOARD_UPDATED_EVENT).data(dashboardService.getSummary())
		);
	}

	private void sendEvent(String emitterId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
		} catch (IOException | IllegalStateException exception) {
			removeEmitter(emitterId);
		}
	}

	private void removeEmitter(String emitterId) {
		emitters.remove(emitterId);
	}
}
