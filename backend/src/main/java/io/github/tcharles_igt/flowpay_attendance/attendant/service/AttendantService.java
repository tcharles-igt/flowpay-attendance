package io.github.tcharles_igt.flowpay_attendance.attendant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantStatusRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantUpdateRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantResponse;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.exception.BadRequestException;
import io.github.tcharles_igt.flowpay_attendance.shared.exception.ResourceNotFoundException;

@Service
public class AttendantService {

	private final AttendantRepository attendantRepository;

	private final AttendanceRepository attendanceRepository;

	public AttendantService(
		AttendantRepository attendantRepository,
		AttendanceRepository attendanceRepository
	) {
		this.attendantRepository = attendantRepository;
		this.attendanceRepository = attendanceRepository;
	}

	@Transactional
	public AttendantResponse create(AttendantRequest request) {
		var attendant = new Attendant();
		attendant.setName(request.name().trim());
		attendant.setTeam(request.team());
		attendant.setActive(resolveActive(request.active(), true));

		return findResponseById(attendantRepository.save(attendant).getId());
	}

	@Transactional
	public AttendantResponse update(Long id, AttendantUpdateRequest request) {
		var attendant = findEntityById(id);
		var nextActive = resolveActive(request.active(), attendant.isActive());
		validateDeactivation(attendant, nextActive);

		attendant.setName(request.name().trim());
		attendant.setTeam(request.team());
		attendant.setActive(nextActive);

		return findResponseById(attendant.getId());
	}

	@Transactional
	public AttendantResponse updateStatus(Long id, AttendantStatusRequest request) {
		var attendant = findEntityById(id);
		validateDeactivation(attendant, request.active());
		attendant.setActive(request.active());
		return findResponseById(attendant.getId());
	}

	@Transactional(readOnly = true)
	public List<AttendantResponse> findAll() {
		return attendantRepository.findOperationalSnapshots(
			AttendanceStatus.IN_PROGRESS,
			AttendantCapacityPolicy.MAX_SIMULTANEOUS_ATTENDANCES
		)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public AttendantResponse findById(Long id) {
		return findResponseById(id);
	}

	private Attendant findEntityById(Long id) {
		return attendantRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Attendant not found: " + id));
	}

	private AttendantResponse findResponseById(Long id) {
		return attendantRepository.findOperationalSnapshotById(
			id,
			AttendanceStatus.IN_PROGRESS,
			AttendantCapacityPolicy.MAX_SIMULTANEOUS_ATTENDANCES
		)
			.map(this::toResponse)
			.orElseThrow(() -> new ResourceNotFoundException("Attendant not found: " + id));
	}

	private void validateDeactivation(Attendant attendant, boolean nextActive) {
		if (attendant.isActive() && !nextActive
			&& attendanceRepository.countByAttendantIdAndStatus(attendant.getId(), AttendanceStatus.IN_PROGRESS) > 0) {
			throw new BadRequestException("Cannot deactivate attendant with attendances in progress");
		}
	}

	private boolean resolveActive(Boolean active, boolean fallback) {
		return active != null ? active : fallback;
	}

	private AttendantResponse toResponse(io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantOperationalSnapshot attendant) {
		return new AttendantResponse(
			attendant.id(),
			attendant.name(),
			attendant.team(),
			attendant.active(),
			attendant.activeAttendances(),
			attendant.availableSlots(),
			attendant.createdAt(),
			attendant.updatedAt()
		);
	}
}
