package io.github.tcharles_igt.flowpay_attendance.dashboard.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.service.AttendantCapacityPolicy;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardAttendantResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardInProgressAttendanceResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardQueueItemResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardTeamSummaryResponse;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@Service
public class DashboardService {

	private final AttendanceRepository attendanceRepository;

	private final AttendantRepository attendantRepository;

	private final Clock clock;

	public DashboardService(
		AttendanceRepository attendanceRepository,
		AttendantRepository attendantRepository,
		Clock clock
	) {
		this.attendanceRepository = attendanceRepository;
		this.attendantRepository = attendantRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DashboardResponse getSummary() {
		var referenceTime = OffsetDateTime.now(clock);
		var attendances = attendanceRepository.findAll();
		var waiting = countByStatus(attendances, AttendanceStatus.WAITING);
		var inProgress = countByStatus(attendances, AttendanceStatus.IN_PROGRESS);
		var finished = countByStatus(attendances, AttendanceStatus.FINISHED);
		var activeLoadByAttendantId = attendances.stream()
			.filter(attendance -> attendance.getStatus() == AttendanceStatus.IN_PROGRESS)
			.map(Attendance::getAttendant)
			.filter(Objects::nonNull)
			.collect(Collectors.groupingBy(attendant -> attendant.getId(), Collectors.counting()));

		var teams = Arrays.stream(TeamType.values())
			.map(team -> toTeamSummary(team, attendances, referenceTime))
			.toList();

		var attendants = attendantRepository.findAllByOrderByNameAsc()
			.stream()
			.map(attendant -> {
				var activeAttendances = activeLoadByAttendantId.getOrDefault(attendant.getId(), 0L);
				return new DashboardAttendantResponse(
					attendant.getId(),
					attendant.getName(),
					attendant.getTeam(),
					activeAttendances,
					attendant.isActive() ? Math.max(0, AttendantCapacityPolicy.MAX_SIMULTANEOUS_ATTENDANCES - activeAttendances) : 0
				);
			})
			.toList();

		var queue = attendances.stream()
			.filter(attendance -> attendance.getStatus() == AttendanceStatus.WAITING)
			.sorted(Comparator.comparing(Attendance::getCreatedAt))
			.map(attendance -> new DashboardQueueItemResponse(
				attendance.getId(),
				attendance.getCustomerName(),
				attendance.getSubject(),
				attendance.getTeam(),
				attendance.getStatus(),
				attendance.getCreatedAt()
			))
			.toList();

		var inProgressAttendances = attendances.stream()
			.filter(attendance -> attendance.getStatus() == AttendanceStatus.IN_PROGRESS)
			.sorted(Comparator.comparing(Attendance::getStartedAt))
			.map(attendance -> new DashboardInProgressAttendanceResponse(
				attendance.getId(),
				attendance.getCustomerName(),
				attendance.getSubject(),
				attendance.getTeam(),
				attendance.getStatus(),
				attendance.getAttendant() != null ? attendance.getAttendant().getId() : null,
				attendance.getAttendant() != null ? attendance.getAttendant().getName() : null,
				attendance.getStartedAt()
			))
			.toList();

		return new DashboardResponse(
			waiting + inProgress + finished,
			waiting,
			inProgress,
			finished,
			calculateAverageQueueTimeMinutes(attendances, referenceTime),
			calculateAverageServiceTimeMinutes(attendances, referenceTime),
			teams,
			attendants,
			queue,
			inProgressAttendances
		);
	}

	private DashboardTeamSummaryResponse toTeamSummary(
		TeamType team,
		List<Attendance> attendances,
		OffsetDateTime referenceTime
	) {
		var teamAttendances = attendances.stream()
			.filter(attendance -> attendance.getTeam() == team)
			.toList();

		return new DashboardTeamSummaryResponse(
			team,
			countByStatus(teamAttendances, AttendanceStatus.WAITING),
			countByStatus(teamAttendances, AttendanceStatus.IN_PROGRESS),
			countByStatus(teamAttendances, AttendanceStatus.FINISHED),
			calculateAverageQueueTimeMinutes(teamAttendances, referenceTime),
			calculateAverageServiceTimeMinutes(teamAttendances, referenceTime)
		);
	}

	private long countByStatus(List<Attendance> attendances, AttendanceStatus status) {
		return attendances.stream()
			.filter(attendance -> attendance.getStatus() == status)
			.count();
	}

	private long calculateAverageQueueTimeMinutes(List<Attendance> attendances, OffsetDateTime referenceTime) {
		return averageMinutes(attendances.stream()
			.map(attendance -> resolveQueueDurationMinutes(attendance, referenceTime))
			.filter(Objects::nonNull)
			.toList());
	}

	private long calculateAverageServiceTimeMinutes(List<Attendance> attendances, OffsetDateTime referenceTime) {
		return averageMinutes(attendances.stream()
			.map(attendance -> resolveServiceDurationMinutes(attendance, referenceTime))
			.filter(Objects::nonNull)
			.toList());
	}

	private Long resolveQueueDurationMinutes(Attendance attendance, OffsetDateTime referenceTime) {
		if (attendance.getCreatedAt() == null) {
			return null;
		}

		var endTime = attendance.getStartedAt() != null ? attendance.getStartedAt()
			: attendance.getStatus() == AttendanceStatus.WAITING ? referenceTime : null;

		if (endTime == null || endTime.isBefore(attendance.getCreatedAt())) {
			return null;
		}

		return Duration.between(attendance.getCreatedAt(), endTime).toMinutes();
	}

	private Long resolveServiceDurationMinutes(Attendance attendance, OffsetDateTime referenceTime) {
		if (attendance.getStartedAt() == null) {
			return null;
		}

		var endTime = attendance.getFinishedAt() != null ? attendance.getFinishedAt()
			: attendance.getStatus() == AttendanceStatus.IN_PROGRESS ? referenceTime : null;

		if (endTime == null || endTime.isBefore(attendance.getStartedAt())) {
			return null;
		}

		return Duration.between(attendance.getStartedAt(), endTime).toMinutes();
	}

	private long averageMinutes(List<Long> samples) {
		if (samples.isEmpty()) {
			return 0;
		}

		return Math.round(samples.stream()
			.mapToLong(Long::longValue)
			.average()
			.orElse(0));
	}
}
