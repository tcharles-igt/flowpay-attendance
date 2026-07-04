package io.github.tcharles_igt.flowpay_attendance.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendance.dto.AttendanceRequest;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;
@SpringBootTest
@Transactional
class AttendanceServiceTest {

	@Autowired
	private AttendanceService attendanceService;

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private AttendantRepository attendantRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldMapCardProblemToCardsAndStartAttendanceWhenCapacityExists() {
		var response = attendanceService.create(new AttendanceRequest("Cliente Cartao", "Cartao bloqueado", AttendanceSubject.CARD_PROBLEM));

		assertThat(response.team()).isEqualTo(TeamType.CARDS);
		assertThat(response.status()).isEqualTo(AttendanceStatus.IN_PROGRESS);
		assertThat(response.attendantId()).isNotNull();
		assertThat(response.attendantName()).isNotBlank();
		assertThat(response.startedAt()).isNotNull();
		assertThat(response.message()).isEqualTo("Cartao bloqueado");
	}

	@Test
	void shouldIgnoreInactiveAttendantsDuringDistribution() {
		var joao = findAttendantByName("Joao");
		joao.setActive(false);
		attendantRepository.save(joao);
		var carla = findAttendantByName("Carla");
		carla.setActive(false);
		attendantRepository.save(carla);

		var response = attendanceService.create(new AttendanceRequest("Cliente Sem Capacidade", "Preciso de ajuda com o cartao", AttendanceSubject.CARD_PROBLEM));

		assertThat(response.team()).isEqualTo(TeamType.CARDS);
		assertThat(response.status()).isEqualTo(AttendanceStatus.WAITING);
		assertThat(response.attendantId()).isNull();
		assertThat(response.attendantName()).isNull();
		assertThat(response.startedAt()).isNull();
	}

	@Test
	void shouldPutAttendanceInWaitingWhenAllAttendantsFromTeamAreFull() {
		fillCapacity(findAttendantByName("Joao"));
		fillCapacity(findAttendantByName("Carla"));

		var response = attendanceService.create(new AttendanceRequest("Cliente Fila", "Nao consegui concluir a compra", AttendanceSubject.CARD_PROBLEM));

		assertThat(response.team()).isEqualTo(TeamType.CARDS);
		assertThat(response.status()).isEqualTo(AttendanceStatus.WAITING);
		assertThat(response.attendantId()).isNull();
		assertThat(response.attendantName()).isNull();
		assertThat(response.startedAt()).isNull();
	}

	@Test
	void shouldChooseAttendantWithLowerActiveLoad() {
		var joao = findAttendantByName("Joao");
		var carla = findAttendantByName("Carla");
		fillCapacity(joao);
		createInProgressAttendance(carla, "Cliente Carla 1");

		var response = attendanceService.create(
			new AttendanceRequest("Cliente Distribuido", "Quero revisar uma transacao", AttendanceSubject.CARD_PROBLEM)
		);

		assertThat(response.attendantId()).isEqualTo(carla.getId());
		assertThat(response.attendantName()).isEqualTo(carla.getName());
		assertThat(response.status()).isEqualTo(AttendanceStatus.IN_PROGRESS);
	}

	@Test
	void shouldTrimAndPersistAttendanceMessage() {
		var response = attendanceService.create(new AttendanceRequest("  Cliente Mensagem  ", "  Mensagem com espacos  ", AttendanceSubject.OTHER));

		var persisted = attendanceRepository.findById(response.id()).orElseThrow();

		assertThat(response.customerName()).isEqualTo("Cliente Mensagem");
		assertThat(response.message()).isEqualTo("Mensagem com espacos");
		assertThat(persisted.getCustomerName()).isEqualTo("Cliente Mensagem");
		assertThat(persisted.getMessage()).isEqualTo("Mensagem com espacos");
	}

	@Test
	void shouldRedistributeOldestWaitingAttendanceFromSameTeamWhenFinishing() {
		var joao = findAttendantByName("Joao");
		createInProgressAttendance(joao, "Joao Cliente 1");
		createInProgressAttendance(joao, "Joao Cliente 2");
		var cardWaiting = createWaitingAttendance("Fila Cartao", AttendanceSubject.CARD_PROBLEM, TeamType.CARDS,
			OffsetDateTime.now().minusMinutes(20));
		var loanWaiting = createWaitingAttendance("Fila Emprestimo", AttendanceSubject.LOAN_REQUEST, TeamType.LOANS,
			OffsetDateTime.now().minusMinutes(30));
		var inProgress = createInProgressAttendance(joao, "Em Atendimento");

		attendanceService.finish(inProgress.getId());

		var finishedAttendance = attendanceRepository.findById(inProgress.getId()).orElseThrow();
		var redistributedCardWaiting = attendanceRepository.findById(cardWaiting.getId()).orElseThrow();
		var untouchedLoanWaiting = attendanceRepository.findById(loanWaiting.getId()).orElseThrow();

		assertThat(finishedAttendance.getStatus()).isEqualTo(AttendanceStatus.FINISHED);
		assertThat(finishedAttendance.getFinishedAt()).isNotNull();
		assertThat(redistributedCardWaiting.getStatus()).isEqualTo(AttendanceStatus.IN_PROGRESS);
		assertThat(redistributedCardWaiting.getCustomerName()).isEqualTo("Fila Cartao");
		assertThat(redistributedCardWaiting.getAttendant()).isNotNull();
		assertThat(redistributedCardWaiting.getStartedAt()).isNotNull();
		assertThat(untouchedLoanWaiting.getStatus()).isEqualTo(AttendanceStatus.WAITING);
		assertThat(untouchedLoanWaiting.getCustomerName()).isEqualTo("Fila Emprestimo");
	}

	private Attendant findAttendantByName(String name) {
		return attendantRepository.findAll().stream()
			.filter(attendant -> attendant.getName().equals(name))
			.findFirst()
			.orElseThrow();
	}

	private void fillCapacity(Attendant attendant) {
		createInProgressAttendance(attendant, attendant.getName() + " Cliente 1");
		createInProgressAttendance(attendant, attendant.getName() + " Cliente 2");
		createInProgressAttendance(attendant, attendant.getName() + " Cliente 3");
	}

	private Attendance createInProgressAttendance(Attendant attendant, String customerName) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setMessage("Mensagem de " + customerName);
		attendance.setSubject(resolveSubject(attendant.getTeam()));
		attendance.setTeam(attendant.getTeam());
		attendance.setStatus(AttendanceStatus.IN_PROGRESS);
		attendance.setAttendant(attendant);
		attendance.setStartedAt(OffsetDateTime.now().minusMinutes(10));
		return attendanceRepository.save(attendance);
	}

	private Attendance createWaitingAttendance(
		String customerName,
		AttendanceSubject subject,
		TeamType team,
		OffsetDateTime createdAt
	) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setMessage("Mensagem de " + customerName);
		attendance.setSubject(subject);
		attendance.setTeam(team);
		attendance.setStatus(AttendanceStatus.WAITING);
		attendance = attendanceRepository.save(attendance);
		attendanceRepository.flush();
		entityManager.createNativeQuery("update attendances set created_at = ? where id = ?")
			.setParameter(1, createdAt)
			.setParameter(2, attendance.getId())
			.executeUpdate();
		entityManager.flush();
		entityManager.clear();
		return attendance;
	}

	private AttendanceSubject resolveSubject(TeamType team) {
		return switch (team) {
			case CARDS -> AttendanceSubject.CARD_PROBLEM;
			case LOANS -> AttendanceSubject.LOAN_REQUEST;
			case OTHERS -> AttendanceSubject.OTHER;
		};
	}
}
