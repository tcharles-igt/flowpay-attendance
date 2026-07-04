package io.github.tcharles_igt.flowpay_attendance.attendant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@DataJpaTest
class AttendantRepositoryTest {

	@Autowired
	private AttendantRepository attendantRepository;

	@Autowired
	private io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository attendanceRepository;

	@Test
	void shouldSeedInitialAttendants() {
		var attendants = attendantRepository.findAll();

		assertThat(attendants).hasSize(3);
		assertThat(attendants)
			.extracting(Attendant::getName)
			.containsExactlyInAnyOrder("Joao", "Maria", "Ana");
	}

	@Test
	void shouldFindAvailableAttendantsByTeamOrderedByLowerLoad() {
		var joao = attendantRepository.findAll().stream()
			.filter(attendant -> attendant.getName().equals("Joao"))
			.findFirst()
			.orElseThrow();
		var leastLoaded = createAttendant("Bruna", TeamType.CARDS);
		var fullCapacity = createAttendant("Caio", TeamType.CARDS);
		var inactive = createAttendant("Inativo", TeamType.CARDS);
		inactive.setActive(false);
		attendantRepository.save(inactive);

		fillCapacity(joao);
		createInProgressAttendance(leastLoaded, "Cliente 1");
		createInProgressAttendance(fullCapacity, "Cliente 2");
		createInProgressAttendance(fullCapacity, "Cliente 3");
		createInProgressAttendance(fullCapacity, "Cliente 4");

		var availableAttendants = attendantRepository.findAvailableByTeam(TeamType.CARDS,
			AttendanceStatus.IN_PROGRESS, 3);

		assertThat(availableAttendants)
			.extracting(Attendant::getName)
			.containsExactly("Bruna");
	}

	@Test
	void shouldReturnOperationalSnapshotsOrderedByName() {
		var alana = createAttendant("Alana", TeamType.LOANS);
		createInProgressAttendance(alana, "Cliente 1");

		var snapshots = attendantRepository.findOperationalSnapshots(AttendanceStatus.IN_PROGRESS, 3);
		var alanaSnapshot = snapshots.stream()
			.filter(snapshot -> snapshot.name().equals("Alana"))
			.findFirst()
			.orElseThrow();

		assertThat(snapshots.getFirst().name()).isEqualTo("Alana");
		assertThat(alanaSnapshot.activeAttendances()).isEqualTo(1);
		assertThat(alanaSnapshot.availableSlots()).isEqualTo(2);
	}

	private Attendant createAttendant(String name, TeamType team) {
		var attendant = new Attendant();
		attendant.setName(name);
		attendant.setTeam(team);
		attendant.setActive(true);
		return attendantRepository.save(attendant);
	}

	private void createInProgressAttendance(Attendant attendant, String customerName) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setMessage("Mensagem de " + customerName);
		attendance.setSubject(AttendanceSubject.CARD_PROBLEM);
		attendance.setTeam(attendant.getTeam());
		attendance.setStatus(AttendanceStatus.IN_PROGRESS);
		attendance.setAttendant(attendant);
		attendance.setStartedAt(OffsetDateTime.now().minusMinutes(10));
		attendanceRepository.save(attendance);
	}

	private void fillCapacity(Attendant attendant) {
		createInProgressAttendance(attendant, attendant.getName() + " Cliente 1");
		createInProgressAttendance(attendant, attendant.getName() + " Cliente 2");
		createInProgressAttendance(attendant, attendant.getName() + " Cliente 3");
	}
}
