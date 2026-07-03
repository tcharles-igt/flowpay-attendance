package io.github.tcharles_igt.flowpay_attendance.attendance.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.github.tcharles_igt.flowpay_attendance.attendance.dto.AttendanceRequest;
import io.github.tcharles_igt.flowpay_attendance.attendance.dto.AttendanceResponse;
import io.github.tcharles_igt.flowpay_attendance.attendance.service.AttendanceService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {

	private final AttendanceService attendanceService;

	public AttendanceController(AttendanceService attendanceService) {
		this.attendanceService = attendanceService;
	}

	@PostMapping
	public ResponseEntity<AttendanceResponse> create(@Valid @RequestBody AttendanceRequest request) {
		var response = attendanceService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(response.id())
			.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public List<AttendanceResponse> findAll() {
		return attendanceService.findAll();
	}

	@GetMapping("/{id}")
	public AttendanceResponse findById(@PathVariable Long id) {
		return attendanceService.findById(id);
	}

	@PatchMapping("/{id}/finish")
	public AttendanceResponse finish(@PathVariable Long id) {
		return attendanceService.finish(id);
	}
}
