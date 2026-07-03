package io.github.tcharles_igt.flowpay_attendance.attendant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;

public interface AttendantRepository extends JpaRepository<Attendant, Long> {
}
