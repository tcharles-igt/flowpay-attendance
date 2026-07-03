package io.github.tcharles_igt.flowpay_attendance.dashboard.service;

import java.util.Arrays;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardAttendantResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardQueueItemResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardResponse;
import io.github.tcharles_igt.flowpay_attendance.dashboard.dto.DashboardTeamSummaryResponse;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@Service
public class DashboardService {

	private static final long MAX_SIMULTANEOUS_ATTENDANCES = 3;

	private final AttendanceRepository attendanceRepository;

	private final AttendantRepository attendantRepository;

	public DashboardService(
		AttendanceRepository attendanceRepository,
		AttendantRepository attendantRepository
	) {
		this.attendanceRepository = attendanceRepository;
		this.attendantRepository = attendantRepository;
	}

	@Transactional(readOnly = true)
	public DashboardResponse getSummary() {
		var waiting = attendanceRepository.countByStatus(AttendanceStatus.WAITING);
		var inProgress = attendanceRepository.countByStatus(AttendanceStatus.IN_PROGRESS);
		var finished = attendanceRepository.countByStatus(AttendanceStatus.FINISHED);

		var teams = Arrays.stream(TeamType.values())
			.map(team -> new DashboardTeamSummaryResponse(
				team,
				attendanceRepository.countByTeamAndStatus(team, AttendanceStatus.WAITING),
				attendanceRepository.countByTeamAndStatus(team, AttendanceStatus.IN_PROGRESS),
				attendanceRepository.countByTeamAndStatus(team, AttendanceStatus.FINISHED)
			))
			.toList();

		var attendants = attendantRepository.findAllByOrderByNameAsc()
			.stream()
			.map(attendant -> {
				var activeAttendances = attendanceRepository.countByAttendantIdAndStatus(attendant.getId(),
					AttendanceStatus.IN_PROGRESS);
				return new DashboardAttendantResponse(
					attendant.getId(),
					attendant.getName(),
					attendant.getTeam(),
					activeAttendances,
					attendant.isActive() ? Math.max(0, MAX_SIMULTANEOUS_ATTENDANCES - activeAttendances) : 0
				);
			})
			.toList();

		var queue = attendanceRepository.findAllByStatusOrderByCreatedAtAsc(AttendanceStatus.WAITING)
			.stream()
			.map(attendance -> new DashboardQueueItemResponse(
				attendance.getId(),
				attendance.getCustomerName(),
				attendance.getSubject(),
				attendance.getTeam(),
				attendance.getStatus(),
				attendance.getCreatedAt()
			))
			.toList();

		return new DashboardResponse(waiting + inProgress + finished, waiting, inProgress, finished, teams, attendants,
			queue);
	}
}
