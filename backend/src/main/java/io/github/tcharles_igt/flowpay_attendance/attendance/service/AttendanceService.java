package io.github.tcharles_igt.flowpay_attendance.attendance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.dto.AttendanceRequest;
import io.github.tcharles_igt.flowpay_attendance.attendance.dto.AttendanceResponse;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.exception.BusinessException;
import io.github.tcharles_igt.flowpay_attendance.shared.exception.ResourceNotFoundException;

@Service
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;

	private final AttendanceDistributionService attendanceDistributionService;

	public AttendanceService(
		AttendanceRepository attendanceRepository,
		AttendanceDistributionService attendanceDistributionService
	) {
		this.attendanceRepository = attendanceRepository;
		this.attendanceDistributionService = attendanceDistributionService;
	}

	@Transactional
	public AttendanceResponse create(AttendanceRequest request) {
		var attendance = new Attendance();
		attendance.setCustomerName(request.customerName());
		attendance.setSubject(request.subject());

		return toResponse(attendanceDistributionService.distributeNewAttendance(attendance));
	}

	@Transactional
	public AttendanceResponse finish(Long attendanceId) {
		var attendance = attendanceRepository.findById(attendanceId)
			.orElseThrow(() -> new ResourceNotFoundException("Attendance not found: " + attendanceId));
		if (attendance.getStatus() != AttendanceStatus.IN_PROGRESS) {
			throw new BusinessException("Only in-progress attendances can be finished");
		}

		return toResponse(attendanceDistributionService.finishAttendance(attendance));
	}

	@Transactional(readOnly = true)
	public List<AttendanceResponse> findAll() {
		return attendanceRepository.findAllByOrderByCreatedAtAsc()
			.stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public AttendanceResponse findById(Long attendanceId) {
		return attendanceRepository.findById(attendanceId)
			.map(this::toResponse)
			.orElseThrow(() -> new ResourceNotFoundException("Attendance not found: " + attendanceId));
	}

	private AttendanceResponse toResponse(Attendance attendance) {
		return new AttendanceResponse(
			attendance.getId(),
			attendance.getCustomerName(),
			attendance.getSubject(),
			attendance.getTeam(),
			attendance.getStatus(),
			attendance.getAttendant() != null ? attendance.getAttendant().getId() : null,
			attendance.getCreatedAt(),
			attendance.getStartedAt(),
			attendance.getFinishedAt()
		);
	}
}
