package io.github.tcharles_igt.flowpay_attendance.attendance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@DataJpaTest
class AttendanceRepositoryTest {

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private AttendantRepository attendantRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldFindOldestWaitingAttendanceByTeam() {
		var attendant = createAttendant("Renata", TeamType.CARDS);
		var newerWaiting = createAttendance("Cliente Novo", TeamType.CARDS, AttendanceStatus.WAITING, null,
			OffsetDateTime.now().minusMinutes(5));
		var oldestWaiting = createAttendance("Cliente Antigo", TeamType.CARDS, AttendanceStatus.WAITING, null,
			OffsetDateTime.now().minusMinutes(15));
		createAttendance("Em Andamento", TeamType.CARDS, AttendanceStatus.IN_PROGRESS, attendant,
			OffsetDateTime.now().minusMinutes(10));
		updateCreatedAt(newerWaiting.getId(), OffsetDateTime.now().minusMinutes(5));
		updateCreatedAt(oldestWaiting.getId(), OffsetDateTime.now().minusMinutes(15));
		entityManager.flush();
		entityManager.clear();

		var nextAttendance = attendanceRepository.findFirstByTeamAndStatusOrderByCreatedAtAsc(TeamType.CARDS,
			AttendanceStatus.WAITING);

		assertThat(nextAttendance).isPresent();
		assertThat(nextAttendance.get().getCustomerName()).isEqualTo(oldestWaiting.getCustomerName());
		assertThat(nextAttendance.get().getId()).isEqualTo(oldestWaiting.getId());
		assertThat(nextAttendance.get().getId()).isNotEqualTo(newerWaiting.getId());
	}

	@Test
	void shouldCountInProgressAttendancesByAttendant() {
		var attendant = createAttendant("Daniel", TeamType.LOANS);
		createAttendance("Cliente 1", TeamType.LOANS, AttendanceStatus.IN_PROGRESS, attendant,
			OffsetDateTime.now().minusMinutes(30));
		createAttendance("Cliente 2", TeamType.LOANS, AttendanceStatus.IN_PROGRESS, attendant,
			OffsetDateTime.now().minusMinutes(20));
		createAttendance("Cliente 3", TeamType.LOANS, AttendanceStatus.FINISHED, attendant,
			OffsetDateTime.now().minusMinutes(10));

		var activeAttendances = attendanceRepository.countByAttendantIdAndStatus(attendant.getId(),
			AttendanceStatus.IN_PROGRESS);

		assertThat(activeAttendances).isEqualTo(2);
	}

	private Attendant createAttendant(String name, TeamType team) {
		var attendant = new Attendant();
		attendant.setName(name);
		attendant.setTeam(team);
		attendant.setActive(true);
		return attendantRepository.save(attendant);
	}

	private Attendance createAttendance(
		String customerName,
		TeamType team,
		AttendanceStatus status,
		Attendant attendant,
		OffsetDateTime startedAt
	) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setMessage("Mensagem de " + customerName);
		attendance.setSubject(AttendanceSubject.OTHER);
		attendance.setTeam(team);
		attendance.setStatus(status);
		attendance.setAttendant(attendant);
		attendance.setStartedAt(startedAt);
		if (status == AttendanceStatus.FINISHED) {
			attendance.setFinishedAt(startedAt.plusMinutes(5));
		}
		return attendanceRepository.save(attendance);
	}

	private void updateCreatedAt(Long attendanceId, OffsetDateTime createdAt) {
		entityManager.createNativeQuery("update attendances set created_at = ? where id = ?")
			.setParameter(1, createdAt)
			.setParameter(2, attendanceId)
			.executeUpdate();
	}
}
