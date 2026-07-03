package io.github.tcharles_igt.flowpay_attendance.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
		when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
	void shouldAssignToAvailableAttendant() {
		var availableAttendant = attendant(10L, "Joao", TeamType.CARDS);
		var attendance = newAttendance("Cliente Disponivel", AttendanceSubject.CARD_PROBLEM);
		when(attendantRepository.findAvailableByTeam(TeamType.CARDS, AttendanceStatus.IN_PROGRESS, 3))
			.thenReturn(List.of(availableAttendant));

		var distributed = distributionService.distributeNewAttendance(attendance);

		assertThat(distributed.getTeam()).isEqualTo(TeamType.CARDS);
		assertThat(distributed.getStatus()).isEqualTo(AttendanceStatus.IN_PROGRESS);
		assertThat(distributed.getAttendant()).isSameAs(availableAttendant);
		assertThat(distributed.getStartedAt()).isNotNull();
		verify(attendanceRepository).save(attendance);
	}

	@Test
	void shouldNotExceedMaximumSimultaneousAttendances() {
		var attendance = newAttendance("Cliente Limite", AttendanceSubject.CARD_PROBLEM);
		when(attendantRepository.findAvailableByTeam(TeamType.CARDS, AttendanceStatus.IN_PROGRESS, 3))
			.thenReturn(List.of());

		var distributed = distributionService.distributeNewAttendance(attendance);

		assertThat(distributed.getStatus()).isEqualTo(AttendanceStatus.WAITING);
		assertThat(distributed.getAttendant()).isNull();
		assertThat(distributed.getStartedAt()).isNull();
		verify(attendantRepository).findAvailableByTeam(TeamType.CARDS, AttendanceStatus.IN_PROGRESS, 3);
	}

	@Test
	void shouldSendToWaitingWhenNoAttendantHasCapacity() {
		var attendance = newAttendance("Cliente Fila", AttendanceSubject.LOAN_REQUEST);
		when(attendantRepository.findAvailableByTeam(TeamType.LOANS, AttendanceStatus.IN_PROGRESS, 3))
			.thenReturn(List.of());

		var distributed = distributionService.distributeNewAttendance(attendance);

		assertThat(distributed.getTeam()).isEqualTo(TeamType.LOANS);
		assertThat(distributed.getStatus()).isEqualTo(AttendanceStatus.WAITING);
		assertThat(distributed.getAttendant()).isNull();
		assertThat(distributed.getStartedAt()).isNull();
	}

	@Test
	void shouldRedistributeOldestWaitingAttendanceWhenFinishing() {
		var releasedAttendant = attendant(20L, "Carla", TeamType.CARDS);
		var inProgress = new Attendance();
		inProgress.setTeam(TeamType.CARDS);
		inProgress.setStatus(AttendanceStatus.IN_PROGRESS);
		inProgress.setAttendant(releasedAttendant);
		inProgress.setStartedAt(OffsetDateTime.now().minusMinutes(8));

		var waiting = newAttendance("Fila Cartao", AttendanceSubject.CARD_PROBLEM);
		waiting.setTeam(TeamType.CARDS);
		waiting.setStatus(AttendanceStatus.WAITING);

		when(attendanceRepository.countByAttendantIdAndStatus(releasedAttendant.getId(), AttendanceStatus.IN_PROGRESS))
			.thenReturn(2L);
		when(attendanceRepository.findFirstByTeamAndStatusOrderByCreatedAtAsc(TeamType.CARDS, AttendanceStatus.WAITING))
			.thenReturn(Optional.of(waiting));

		var finished = distributionService.finishAttendance(inProgress);

		assertThat(finished.getStatus()).isEqualTo(AttendanceStatus.FINISHED);
		assertThat(finished.getFinishedAt()).isNotNull();
		assertThat(waiting.getStatus()).isEqualTo(AttendanceStatus.IN_PROGRESS);
		assertThat(waiting.getAttendant()).isSameAs(releasedAttendant);
		assertThat(waiting.getStartedAt()).isNotNull();
		verify(attendanceRepository).findFirstByTeamAndStatusOrderByCreatedAtAsc(TeamType.CARDS, AttendanceStatus.WAITING);
		verify(attendanceRepository).save(waiting);
	}

	@Test
	void shouldKeepQueueSeparatedByTeamWhenFinishing() {
		var releasedAttendant = attendant(30L, "Julia", TeamType.CARDS);
		var inProgress = new Attendance();
		inProgress.setTeam(TeamType.CARDS);
		inProgress.setStatus(AttendanceStatus.IN_PROGRESS);
		inProgress.setAttendant(releasedAttendant);

		when(attendanceRepository.countByAttendantIdAndStatus(releasedAttendant.getId(), AttendanceStatus.IN_PROGRESS))
			.thenReturn(1L);
		when(attendanceRepository.findFirstByTeamAndStatusOrderByCreatedAtAsc(TeamType.CARDS, AttendanceStatus.WAITING))
			.thenReturn(Optional.empty());

		distributionService.finishAttendance(inProgress);

		verify(attendanceRepository).findFirstByTeamAndStatusOrderByCreatedAtAsc(TeamType.CARDS, AttendanceStatus.WAITING);
		verify(attendanceRepository, never())
			.findFirstByTeamAndStatusOrderByCreatedAtAsc(TeamType.LOANS, AttendanceStatus.WAITING);
		verify(attendanceRepository).save(inProgress);
	}

	@Test
	void shouldNotRedistributeWhenReleasedAttendantIsStillAtCapacity() {
		var releasedAttendant = attendant(40L, "Rafa", TeamType.LOANS);
		var inProgress = new Attendance();
		inProgress.setTeam(TeamType.LOANS);
		inProgress.setStatus(AttendanceStatus.IN_PROGRESS);
		inProgress.setAttendant(releasedAttendant);

		when(attendanceRepository.countByAttendantIdAndStatus(releasedAttendant.getId(), AttendanceStatus.IN_PROGRESS))
			.thenReturn(3L);

		distributionService.finishAttendance(inProgress);

		verify(attendanceRepository, never())
			.findFirstByTeamAndStatusOrderByCreatedAtAsc(eq(TeamType.LOANS), eq(AttendanceStatus.WAITING));
	}

	private Attendance newAttendance(String customerName, AttendanceSubject subject) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
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
