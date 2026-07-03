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
import io.github.tcharles_igt.flowpay_attendance.shared.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendances")
@Tag(name = "Attendances", description = "Operacoes de abertura, consulta e finalizacao de atendimentos.")
public class AttendanceController {

	private final AttendanceService attendanceService;

	public AttendanceController(AttendanceService attendanceService) {
		this.attendanceService = attendanceService;
	}

	@PostMapping
	@Operation(
		summary = "Abrir novo atendimento",
		description = "Cria um atendimento, identifica o time responsavel e tenta distribuicao imediata conforme a capacidade."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Atendimento criado com sucesso."),
		@ApiResponse(
			responseCode = "400",
			description = "Payload invalido.",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
		)
	})
	public ResponseEntity<AttendanceResponse> create(@Valid @RequestBody AttendanceRequest request) {
		var response = attendanceService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(response.id())
			.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	@Operation(summary = "Listar atendimentos", description = "Retorna todos os atendimentos ordenados pela data de criacao.")
	public List<AttendanceResponse> findAll() {
		return attendanceService.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar atendimento por ID")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Atendimento encontrado."),
		@ApiResponse(
			responseCode = "404",
			description = "Atendimento nao encontrado.",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
		)
	})
	public AttendanceResponse findById(@PathVariable Long id) {
		return attendanceService.findById(id);
	}

	@PatchMapping("/{id}/finish")
	@Operation(
		summary = "Finalizar atendimento",
		description = "Encerra um atendimento em andamento e tenta redistribuir o item WAITING mais antigo do mesmo time."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Atendimento finalizado com sucesso."),
		@ApiResponse(
			responseCode = "404",
			description = "Atendimento nao encontrado.",
			content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "422",
			description = "Estado invalido para finalizacao.",
			content = @Content(
				schema = @Schema(implementation = ApiErrorResponse.class),
				examples = @ExampleObject(
					value = """
						{
						  "timestamp": "2026-07-03T12:00:00Z",
						  "status": 422,
						  "error": "Unprocessable Entity",
						  "message": "Only in-progress attendances can be finished",
						  "details": []
						}
						"""
				)
			)
		)
	})
	public AttendanceResponse finish(@PathVariable Long id) {
		return attendanceService.finish(id);
	}
}
