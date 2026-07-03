package io.github.tcharles_igt.flowpay_attendance.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Transactional
@Import(DashboardServiceTest.FixedClockConfiguration.class)
class DashboardServiceTest {

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private AttendantRepository attendantRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldCalculateAverageTimesForDashboardAndTeams() {
		var joao = findAttendantByName("Joao");
		var maria = findAttendantByName("Maria");

		createAttendance(
			"Fila Cartoes",
			AttendanceStatus.WAITING,
			AttendanceSubject.CARD_PROBLEM,
			TeamType.CARDS,
			null,
			OffsetDateTime.parse("2026-07-03T11:45:00Z"),
			null,
			null
		);
		createAttendance(
			"Em atendimento cartoes",
			AttendanceStatus.IN_PROGRESS,
			AttendanceSubject.CARD_PROBLEM,
			TeamType.CARDS,
			joao,
			OffsetDateTime.parse("2026-07-03T11:30:00Z"),
			OffsetDateTime.parse("2026-07-03T11:40:00Z"),
			null
		);
		createAttendance(
			"Finalizado emprestimos",
			AttendanceStatus.FINISHED,
			AttendanceSubject.LOAN_REQUEST,
			TeamType.LOANS,
			maria,
			OffsetDateTime.parse("2026-07-03T11:00:00Z"),
			OffsetDateTime.parse("2026-07-03T11:05:00Z"),
			OffsetDateTime.parse("2026-07-03T11:25:00Z")
		);

		var response = dashboardService.getSummary();
		var cardsSummary = response.teams().stream().filter(team -> team.team() == TeamType.CARDS).findFirst().orElseThrow();
		var loansSummary = response.teams().stream().filter(team -> team.team() == TeamType.LOANS).findFirst().orElseThrow();
		var othersSummary = response.teams().stream().filter(team -> team.team() == TeamType.OTHERS).findFirst().orElseThrow();

		assertThat(response.totalAttendances()).isEqualTo(3);
		assertThat(response.waiting()).isEqualTo(1);
		assertThat(response.inProgress()).isEqualTo(1);
		assertThat(response.finished()).isEqualTo(1);
		assertThat(response.averageQueueTimeMinutes()).isEqualTo(10);
		assertThat(response.averageServiceTimeMinutes()).isEqualTo(20);

		assertThat(cardsSummary.waiting()).isEqualTo(1);
		assertThat(cardsSummary.inProgress()).isEqualTo(1);
		assertThat(cardsSummary.finished()).isZero();
		assertThat(cardsSummary.averageQueueTimeMinutes()).isEqualTo(13);
		assertThat(cardsSummary.averageServiceTimeMinutes()).isEqualTo(20);

		assertThat(loansSummary.waiting()).isZero();
		assertThat(loansSummary.inProgress()).isZero();
		assertThat(loansSummary.finished()).isEqualTo(1);
		assertThat(loansSummary.averageQueueTimeMinutes()).isEqualTo(5);
		assertThat(loansSummary.averageServiceTimeMinutes()).isEqualTo(20);

		assertThat(othersSummary.averageQueueTimeMinutes()).isZero();
		assertThat(othersSummary.averageServiceTimeMinutes()).isZero();
	}

	private Attendant findAttendantByName(String name) {
		return attendantRepository.findAll().stream()
			.filter(attendant -> attendant.getName().equals(name))
			.findFirst()
			.orElseThrow();
	}

	private void createAttendance(
		String customerName,
		AttendanceStatus status,
		AttendanceSubject subject,
		TeamType team,
		Attendant attendant,
		OffsetDateTime createdAt,
		OffsetDateTime startedAt,
		OffsetDateTime finishedAt
	) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setStatus(status);
		attendance.setSubject(subject);
		attendance.setTeam(team);
		attendance.setAttendant(attendant);
		attendance.setStartedAt(startedAt);
		attendance.setFinishedAt(finishedAt);
		attendance = attendanceRepository.save(attendance);
		attendanceRepository.flush();

		entityManager.createNativeQuery(
			"update attendances set created_at = ?, started_at = ?, finished_at = ?, updated_at = ? where id = ?"
		)
			.setParameter(1, createdAt)
			.setParameter(2, startedAt)
			.setParameter(3, finishedAt)
			.setParameter(4, createdAt)
			.setParameter(5, attendance.getId())
			.executeUpdate();
		entityManager.flush();
		entityManager.clear();
	}

	@TestConfiguration
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock applicationClock() {
			return Clock.fixed(Instant.parse("2026-07-03T12:00:00Z"), ZoneOffset.UTC);
		}
	}
}
