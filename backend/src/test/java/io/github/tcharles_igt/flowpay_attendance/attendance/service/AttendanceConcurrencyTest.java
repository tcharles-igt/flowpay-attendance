package io.github.tcharles_igt.flowpay_attendance.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendance.dto.AttendanceRequest;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AttendanceConcurrencyTest {

	@Autowired
	private AttendanceService attendanceService;

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private AttendantRepository attendantRepository;

	@Test
	void shouldRespectSingleAttendantCapacityUnderConcurrentRequests() throws Exception {
		var joao = attendantRepository.findAll().stream()
			.filter(attendant -> attendant.getName().equals("Joao"))
			.findFirst()
			.orElseThrow();

		int requestCount = 6;
		var startGate = new CountDownLatch(1);
		var executor = Executors.newFixedThreadPool(requestCount);
		List<Callable<Void>> tasks = new ArrayList<>();
		for (int index = 0; index < requestCount; index++) {
			final int requestIndex = index;
			tasks.add(() -> {
				startGate.await(5, TimeUnit.SECONDS);
				attendanceService.create(
					new AttendanceRequest(
						"Cliente Concorrente " + requestIndex,
						"Mensagem concorrente " + requestIndex,
						AttendanceSubject.CARD_PROBLEM
					)
				);
				return null;
			});
		}

		try {
			List<Future<Void>> futures = tasks.stream()
				.map(executor::submit)
				.toList();
			startGate.countDown();

			for (Future<Void> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}

			assertThat(attendanceRepository.countByAttendantIdAndStatus(joao.getId(), AttendanceStatus.IN_PROGRESS)).isEqualTo(3);
			assertThat(attendanceRepository.countByTeamAndStatus(TeamType.CARDS, AttendanceStatus.IN_PROGRESS)).isEqualTo(3);
			assertThat(attendanceRepository.countByTeamAndStatus(TeamType.CARDS, AttendanceStatus.WAITING)).isEqualTo(3);
		} finally {
			executor.shutdownNow();
			assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
			attendanceRepository.deleteAllInBatch();
		}
	}
}
