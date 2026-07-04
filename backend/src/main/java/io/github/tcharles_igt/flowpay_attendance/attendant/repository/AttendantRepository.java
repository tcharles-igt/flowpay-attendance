package io.github.tcharles_igt.flowpay_attendance.attendant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantOperationalSnapshot;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

public interface AttendantRepository extends JpaRepository<Attendant, Long> {

	List<Attendant> findAllByOrderByNameAsc();

	List<Attendant> findAllByActiveTrueOrderByNameAsc();

	List<Attendant> findAllByTeamAndActiveTrueOrderByNameAsc(TeamType team);

	@Query("""
		select new io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantOperationalSnapshot(
			attendant.id,
			attendant.name,
			attendant.team,
			attendant.active,
			count(attendance.id),
			case
				when attendant.active = true and count(attendance.id) < :maxAttendances then :maxAttendances - count(attendance.id)
				when attendant.active = true then 0L
				else 0L
			end,
			attendant.createdAt,
			attendant.updatedAt
		)
		from Attendant attendant
		left join attendant.attendances attendance
			on attendance.status = :inProgressStatus
		group by attendant.id, attendant.name, attendant.team, attendant.active, attendant.createdAt, attendant.updatedAt
		order by attendant.name asc
		""")
	List<AttendantOperationalSnapshot> findOperationalSnapshots(
		@Param("inProgressStatus") AttendanceStatus inProgressStatus,
		@Param("maxAttendances") long maxAttendances
	);

	@Query("""
		select new io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantOperationalSnapshot(
			attendant.id,
			attendant.name,
			attendant.team,
			attendant.active,
			count(attendance.id),
			case
				when attendant.active = true and count(attendance.id) < :maxAttendances then :maxAttendances - count(attendance.id)
				when attendant.active = true then 0L
				else 0L
			end,
			attendant.createdAt,
			attendant.updatedAt
		)
		from Attendant attendant
		left join attendant.attendances attendance
			on attendance.status = :inProgressStatus
		where attendant.id = :id
		group by attendant.id, attendant.name, attendant.team, attendant.active, attendant.createdAt, attendant.updatedAt
		""")
	Optional<AttendantOperationalSnapshot> findOperationalSnapshotById(
		@Param("id") Long id,
		@Param("inProgressStatus") AttendanceStatus inProgressStatus,
		@Param("maxAttendances") long maxAttendances
	);

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

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select attendant
		from Attendant attendant
		where attendant.active = true
			and attendant.team = :team
		order by attendant.id
		""")
	List<Attendant> findActiveByTeamForUpdate(@Param("team") TeamType team);
}
