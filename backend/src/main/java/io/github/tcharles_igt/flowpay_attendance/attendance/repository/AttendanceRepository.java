package io.github.tcharles_igt.flowpay_attendance.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
}
