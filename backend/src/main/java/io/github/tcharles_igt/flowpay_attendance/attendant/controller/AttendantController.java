package io.github.tcharles_igt.flowpay_attendance.attendant.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantResponse;
import io.github.tcharles_igt.flowpay_attendance.attendant.service.AttendantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendants")
@Tag(name = "Attendants", description = "Gestao basica de atendentes disponiveis para distribuicao.")
public class AttendantController {

	private final AttendantService attendantService;

	public AttendantController(AttendantService attendantService) {
		this.attendantService = attendantService;
	}

	@PostMapping
	@Operation(summary = "Criar atendente")
	public ResponseEntity<AttendantResponse> create(@Valid @RequestBody AttendantRequest request) {
		var response = attendantService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(response.id())
			.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	@Operation(summary = "Listar atendentes")
	public List<AttendantResponse> findAll() {
		return attendantService.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar atendente por ID")
	public AttendantResponse findById(@PathVariable Long id) {
		return attendantService.findById(id);
	}
}
