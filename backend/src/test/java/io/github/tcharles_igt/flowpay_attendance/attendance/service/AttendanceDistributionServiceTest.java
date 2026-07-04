package io.github.tcharles_igt.flowpay_attendance.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;
@ExtendWith(MockitoExtension.class)
class AttendanceDistributionServiceTest {

	@Mock
	private AttendantRepository attendantRepository;

	@Mock
	private AttendanceRepository attendanceRepository;

	@InjectMocks
	private AttendanceDistributionService distributionService;

	@BeforeEach
	void setUp() {
		lenient().when(attendanceRepository.save(any(Attendance.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void shouldResolveCardProblemToCards() {
		assertThat(distributionService.resolveTeam(AttendanceSubject.CARD_PROBLEM)).isEqualTo(TeamType.CARDS);
	}

	@Test
	void shouldResolveLoanRequestToLoans() {
		assertThat(distributionService.resolveTeam(AttendanceSubject.LOAN_REQUEST)).isEqualTo(TeamType.LOANS);
	}

	@Test
	void shouldResolveOtherToOthers() {
		assertThat(distributionService.resolveTeam(AttendanceSubject.OTHER)).isEqualTo(TeamType.OTHERS);
	}

	@Test
	void shouldCreateWaitingAttendanceWhenNoCapacityExists() {
		var attendance = newAttendance("Cliente Disponivel", AttendanceSubject.CARD_PROBLEM);
		when(attendantRepository.findAvailableByTeam(TeamType.CARDS, AttendanceStatus.IN_PROGRESS, 3))
			.thenReturn(List.of());

		var queued = distributionService.distributeNewAttendance(attendance);

		assertThat(queued.getTeam()).isEqualTo(TeamType.CARDS);
		assertThat(queued.getStatus()).isEqualTo(AttendanceStatus.WAITING);
		assertThat(queued.getAttendant()).isNull();
		assertThat(queued.getStartedAt()).isNull();
		verify(attendanceRepository).save(attendance);
	}

	@Test
	void shouldCreateInProgressAttendanceWhenCapacityExists() {
		var availableAttendant = attendant(10L, "Joao", TeamType.CARDS);
		var attendance = newAttendance("Cliente Disponivel", AttendanceSubject.CARD_PROBLEM);
		when(attendantRepository.findAvailableByTeam(TeamType.CARDS, AttendanceStatus.IN_PROGRESS, 3))
			.thenReturn(List.of(availableAttendant));

		var distributed = distributionService.distributeNewAttendance(attendance);

		assertThat(distributed.getStatus()).isEqualTo(AttendanceStatus.IN_PROGRESS);
		assertThat(distributed.getAttendant()).isSameAs(availableAttendant);
		assertThat(distributed.getStartedAt()).isNotNull();
		verify(attendanceRepository).save(attendance);
	}

	@Test
	void shouldFinishAttendanceAndRedistributeOldestWaitingItemFromSameTeam() {
		var availableAttendant = attendant(10L, "Joao", TeamType.CARDS);
		var inProgress = new Attendance();
		inProgress.setTeam(TeamType.CARDS);
		inProgress.setMessage("Mensagem em atendimento");
		inProgress.setStatus(AttendanceStatus.IN_PROGRESS);
		var waiting = new Attendance();
		waiting.setTeam(TeamType.CARDS);
		waiting.setMessage("Mensagem em fila");
		waiting.setStatus(AttendanceStatus.WAITING);
		when(attendanceRepository.findFirstByTeamAndStatusOrderByCreatedAtAsc(TeamType.CARDS, AttendanceStatus.WAITING))
			.thenReturn(java.util.Optional.of(waiting));
		when(attendantRepository.findAvailableByTeam(TeamType.CARDS, AttendanceStatus.IN_PROGRESS, 3))
			.thenReturn(List.of(availableAttendant));

		var finished = distributionService.finishAttendance(inProgress);

		assertThat(finished.getStatus()).isEqualTo(AttendanceStatus.FINISHED);
		assertThat(finished.getFinishedAt()).isNotNull();
		verify(attendanceRepository).save(inProgress);
		assertThat(waiting.getStatus()).isEqualTo(AttendanceStatus.IN_PROGRESS);
		assertThat(waiting.getAttendant()).isSameAs(availableAttendant);
		assertThat(waiting.getStartedAt()).isNotNull();
		verify(attendanceRepository).save(waiting);
	}

	private Attendance newAttendance(String customerName, AttendanceSubject subject) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setMessage("Mensagem de " + customerName);
		attendance.setSubject(subject);
		return attendance;
	}

	private Attendant attendant(Long id, String name, TeamType team) {
		var attendant = new Attendant();
		setId(attendant, id);
		attendant.setName(name);
		attendant.setTeam(team);
		attendant.setActive(true);
		return attendant;
	}

	private void setId(Attendant attendant, Long id) {
		try {
			Field field = Attendant.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(attendant, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Unable to set test attendant id", exception);
		}
	}
}
