package io.github.tcharles_igt.flowpay_attendance.attendance.service;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@Service
public class AttendanceDistributionService {

	private static final int MAX_SIMULTANEOUS_ATTENDANCES = 3;

	private final AttendantRepository attendantRepository;

	private final AttendanceRepository attendanceRepository;

	public AttendanceDistributionService(
		AttendantRepository attendantRepository,
		AttendanceRepository attendanceRepository
	) {
		this.attendantRepository = attendantRepository;
		this.attendanceRepository = attendanceRepository;
	}

	public TeamType resolveTeam(AttendanceSubject subject) {
		return switch (subject) {
			case CARD_PROBLEM -> TeamType.CARDS;
			case LOAN_REQUEST -> TeamType.LOANS;
			case OTHER -> TeamType.OTHERS;
		};
	}

	@Transactional
	public Attendance distributeNewAttendance(Attendance attendance) {
		attendance.setTeam(resolveTeam(attendance.getSubject()));
		assignIfPossible(attendance);
		return attendanceRepository.save(attendance);
	}

	@Transactional
	public Attendance finishAttendance(Attendance attendance) {
		if (attendance.getStatus() == AttendanceStatus.FINISHED) {
			return attendance;
		}

		var releasedAttendant = attendance.getAttendant();
		var team = attendance.getTeam();

		attendance.setStatus(AttendanceStatus.FINISHED);
		attendance.setFinishedAt(OffsetDateTime.now());
		attendanceRepository.save(attendance);

		if (releasedAttendant != null) {
			redistributeNextWaitingAttendance(team, releasedAttendant);
		}

		return attendance;
	}

	void assignIfPossible(Attendance attendance) {
		var availableAttendant = findAvailableAttendant(attendance.getTeam());
		if (availableAttendant.isPresent()) {
			assignAttendance(attendance, availableAttendant.get());
			return;
		}

		attendance.setAttendant(null);
		attendance.setStatus(AttendanceStatus.WAITING);
		attendance.setStartedAt(null);
	}

	private Optional<Attendant> findAvailableAttendant(TeamType team) {
		return attendantRepository.findAvailableByTeam(team, AttendanceStatus.IN_PROGRESS, MAX_SIMULTANEOUS_ATTENDANCES)
			.stream()
			.findFirst();
	}

	private void assignAttendance(Attendance attendance, Attendant attendant) {
		attendance.setAttendant(attendant);
		attendance.setStatus(AttendanceStatus.IN_PROGRESS);
		attendance.setStartedAt(OffsetDateTime.now());
		attendance.setFinishedAt(null);
	}

	private void redistributeNextWaitingAttendance(TeamType team, Attendant attendant) {
		if (attendanceRepository.countByAttendantIdAndStatus(attendant.getId(), AttendanceStatus.IN_PROGRESS)
			>= MAX_SIMULTANEOUS_ATTENDANCES) {
			return;
		}

		attendanceRepository.findFirstByTeamAndStatusOrderByCreatedAtAsc(team, AttendanceStatus.WAITING)
			.ifPresent(waitingAttendance -> {
				assignAttendance(waitingAttendance, attendant);
				attendanceRepository.save(waitingAttendance);
			});
	}
}
