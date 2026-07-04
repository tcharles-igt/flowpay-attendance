package io.github.tcharles_igt.flowpay_attendance.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	@Query(
		value = """
			select *
			from attendances
			where team = :team
				and status = :status
			order by created_at
			limit 1
			for update
			""",
		nativeQuery = true
	)
	Optional<Attendance> findFirstWaitingByTeamAndStatusForUpdate(
		@Param("team") String team,
		@Param("status") String status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select attendance from Attendance attendance where attendance.id = :id")
	Optional<Attendance> findByIdForUpdate(@Param("id") Long id);

	long countByAttendantIdAndStatus(Long attendantId, AttendanceStatus status);

	long countByStatus(AttendanceStatus status);

	long countByTeamAndStatus(TeamType team, AttendanceStatus status);

	List<Attendance> findAllByOrderByCreatedAtAsc();

	List<Attendance> findAllByStatusOrderByCreatedAtAsc(AttendanceStatus status);

	List<Attendance> findAllByStatusOrderByStartedAtAsc(AttendanceStatus status);
}
