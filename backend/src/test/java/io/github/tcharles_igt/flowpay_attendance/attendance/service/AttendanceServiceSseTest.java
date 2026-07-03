package io.github.tcharles_igt.flowpay_attendance.attendance.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendance.dto.AttendanceRequest;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.dashboard.service.DashboardStreamService;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceSseTest {

	@Mock
	private AttendanceRepository attendanceRepository;

	@Mock
	private AttendanceDistributionService attendanceDistributionService;

	@Mock
	private DashboardStreamService dashboardStreamService;

	@InjectMocks
	private AttendanceService attendanceService;

	@Test
	void shouldPublishDashboardUpdateOnlyAfterCreateCommit() {
		var attendance = new Attendance();
		attendance.setCustomerName("Cliente SSE");
		attendance.setSubject(AttendanceSubject.CARD_PROBLEM);
		attendance.setTeam(TeamType.CARDS);
		attendance.setStatus(AttendanceStatus.IN_PROGRESS);
		attendance.setStartedAt(OffsetDateTime.now());
		setId(attendance, 1L);
		when(attendanceDistributionService.distributeNewAttendance(org.mockito.ArgumentMatchers.any(Attendance.class)))
			.thenReturn(attendance);

		TransactionSynchronizationManager.initSynchronization();
		try {
			attendanceService.create(new AttendanceRequest("Cliente SSE", AttendanceSubject.CARD_PROBLEM));

			verify(dashboardStreamService, never()).publishDashboardUpdate();

			TransactionSynchronizationManager.getSynchronizations()
				.forEach(TransactionSynchronization::afterCommit);

			verify(dashboardStreamService).publishDashboardUpdate();
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldPublishDashboardUpdateOnlyAfterFinishCommit() {
		var attendance = new Attendance();
		setId(attendance, 10L);
		attendance.setCustomerName("Cliente Finalizado");
		attendance.setSubject(AttendanceSubject.CARD_PROBLEM);
		attendance.setTeam(TeamType.CARDS);
		attendance.setStatus(AttendanceStatus.IN_PROGRESS);
		attendance.setStartedAt(OffsetDateTime.now().minusMinutes(5));
		when(attendanceRepository.findById(10L)).thenReturn(Optional.of(attendance));
		when(attendanceDistributionService.finishAttendance(attendance)).thenReturn(attendance);

		TransactionSynchronizationManager.initSynchronization();
		try {
			attendanceService.finish(10L);

			verify(dashboardStreamService, never()).publishDashboardUpdate();

			TransactionSynchronizationManager.getSynchronizations()
				.forEach(TransactionSynchronization::afterCommit);

			verify(dashboardStreamService).publishDashboardUpdate();
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private void setId(Attendance attendance, Long id) {
		try {
			Field field = Attendance.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(attendance, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Unable to set test attendance id", exception);
		}
	}
}
