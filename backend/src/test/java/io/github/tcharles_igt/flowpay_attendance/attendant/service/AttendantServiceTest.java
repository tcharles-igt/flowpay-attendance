package io.github.tcharles_igt.flowpay_attendance.attendant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantStatusRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantUpdateRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;
import io.github.tcharles_igt.flowpay_attendance.shared.exception.BadRequestException;

@SpringBootTest
@Transactional
class AttendantServiceTest {

	@Autowired
	private AttendantService attendantService;

	@Autowired
	private AttendantRepository attendantRepository;

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Test
	void shouldCreateAttendantWithDefaultActiveStatusAndAvailableCapacity() {
		var response = attendantService.create(new AttendantRequest("  Patricia  ", TeamType.LOANS, null));

		assertThat(response.name()).isEqualTo("Patricia");
		assertThat(response.active()).isTrue();
		assertThat(response.activeAttendances()).isZero();
		assertThat(response.availableSlots()).isEqualTo(AttendantCapacityPolicy.MAX_SIMULTANEOUS_ATTENDANCES);
	}

	@Test
	void shouldUpdateNameTeamAndActiveFlag() {
		var maria = findAttendantByName("Maria");

		var response = attendantService.update(maria.getId(), new AttendantUpdateRequest("  Maria Oliveira  ", TeamType.OTHERS, false));

		assertThat(response.name()).isEqualTo("Maria Oliveira");
		assertThat(response.team()).isEqualTo(TeamType.OTHERS);
		assertThat(response.active()).isFalse();
		assertThat(response.availableSlots()).isZero();
	}

	@Test
	void shouldReturnOperationalLoadWhenListingAttendants() {
		var joao = findAttendantByName("Joao");
		createInProgressAttendance(joao, "Cliente 1");
		createInProgressAttendance(joao, "Cliente 2");

		var response = attendantService.findById(joao.getId());

		assertThat(response.activeAttendances()).isEqualTo(2);
		assertThat(response.availableSlots()).isEqualTo(1);
	}

	@Test
	void shouldBlockStatusDeactivationWhenAttendantHasInProgressAttendances() {
		var joao = findAttendantByName("Joao");
		createInProgressAttendance(joao, "Cliente em andamento");

		assertThatThrownBy(() -> attendantService.updateStatus(joao.getId(), new AttendantStatusRequest(false)))
			.isInstanceOf(BadRequestException.class)
			.hasMessage("Cannot deactivate attendant with attendances in progress");
	}

	@Test
	void shouldDeactivateAttendantWhenThereIsNoInProgressAttendance() {
		var ana = findAttendantByName("Ana");

		var response = attendantService.updateStatus(ana.getId(), new AttendantStatusRequest(false));

		assertThat(response.active()).isFalse();
		assertThat(response.availableSlots()).isZero();
	}

	private Attendant findAttendantByName(String name) {
		return attendantRepository.findAll().stream()
			.filter(attendant -> attendant.getName().equals(name))
			.findFirst()
			.orElseThrow();
	}

	private void createInProgressAttendance(Attendant attendant, String customerName) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setMessage("Mensagem de " + customerName);
		attendance.setSubject(resolveSubject(attendant.getTeam()));
		attendance.setTeam(attendant.getTeam());
		attendance.setStatus(AttendanceStatus.IN_PROGRESS);
		attendance.setAttendant(attendant);
		attendance.setStartedAt(OffsetDateTime.now().minusMinutes(10));
		attendanceRepository.save(attendance);
	}

	private AttendanceSubject resolveSubject(TeamType team) {
		return switch (team) {
			case CARDS -> AttendanceSubject.CARD_PROBLEM;
			case LOANS -> AttendanceSubject.LOAN_REQUEST;
			case OTHERS -> AttendanceSubject.OTHER;
		};
	}
}
