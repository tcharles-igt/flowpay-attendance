package io.github.tcharles_igt.flowpay_attendance.attendant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantResponse;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.exception.ResourceNotFoundException;

@Service
public class AttendantService {

	private final AttendantRepository attendantRepository;

	public AttendantService(AttendantRepository attendantRepository) {
		this.attendantRepository = attendantRepository;
	}

	@Transactional
	public AttendantResponse create(AttendantRequest request) {
		var attendant = new Attendant();
		attendant.setName(request.name());
		attendant.setTeam(request.team());
		attendant.setActive(request.active());

		return toResponse(attendantRepository.save(attendant));
	}

	@Transactional(readOnly = true)
	public List<AttendantResponse> findAll() {
		return attendantRepository.findAllByOrderByNameAsc()
			.stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public AttendantResponse findById(Long id) {
		return attendantRepository.findById(id)
			.map(this::toResponse)
			.orElseThrow(() -> new ResourceNotFoundException("Attendant not found: " + id));
	}

	private AttendantResponse toResponse(Attendant attendant) {
		return new AttendantResponse(
			attendant.getId(),
			attendant.getName(),
			attendant.getTeam(),
			attendant.isActive(),
			attendant.getCreatedAt(),
			attendant.getUpdatedAt()
		);
	}
}
