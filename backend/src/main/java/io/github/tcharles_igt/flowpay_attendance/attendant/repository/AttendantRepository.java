package io.github.tcharles_igt.flowpay_attendance.attendant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public interface AttendantRepository extends JpaRepository<Attendant, Long> {

	List<Attendant> findAllByActiveTrueOrderByNameAsc();

	List<Attendant> findAllByTeamAndActiveTrueOrderByNameAsc(TeamType team);

	@Query("""
		select attendant
		from Attendant attendant
		left join attendant.attendances attendance
			on attendance.status = :inProgressStatus
		where attendant.active = true
			and attendant.team = :team
		group by attendant
		having count(attendance.id) < :maxAttendances
		order by count(attendance.id), attendant.id
		""")
	List<Attendant> findAvailableByTeam(
		@Param("team") TeamType team,
		@Param("inProgressStatus") AttendanceStatus inProgressStatus,
		@Param("maxAttendances") long maxAttendances
	);
}
