package io.github.tcharles_igt.flowpay_attendance.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	Optional<Attendance> findFirstByTeamAndStatusOrderByCreatedAtAsc(TeamType team, AttendanceStatus status);

	long countByAttendantIdAndStatus(Long attendantId, AttendanceStatus status);

	long countByStatus(AttendanceStatus status);

	long countByTeamAndStatus(TeamType team, AttendanceStatus status);

	List<Attendance> findAllByOrderByCreatedAtAsc();

	List<Attendance> findAllByStatusOrderByCreatedAtAsc(AttendanceStatus status);

	List<Attendance> findAllByStatusOrderByStartedAtAsc(AttendanceStatus status);
}
