package io.github.tcharles_igt.flowpay_attendance.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import io.github.tcharles_igt.flowpay_attendance.attendance.domain.Attendance;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceStatus;
import io.github.tcharles_igt.flowpay_attendance.attendance.domain.AttendanceSubject;
import io.github.tcharles_igt.flowpay_attendance.attendance.repository.AttendanceRepository;
import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.attendant.repository.AttendantRepository;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private AttendantRepository attendantRepository;

	@Test
	void shouldCreateAndReadAttendant() throws Exception {
		mockMvc.perform(post("/api/attendants")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Patricia",
					  "team": "LOANS",
					  "active": true
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/attendants/")))
			.andExpect(jsonPath("$.name").value("Patricia"))
			.andExpect(jsonPath("$.team").value("LOANS"))
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.activeAttendances").value(0))
			.andExpect(jsonPath("$.availableSlots").value(3));

		mockMvc.perform(get("/api/attendants"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.name=='Patricia')]").exists());
	}

	@Test
	void shouldUpdateAttendantAndToggleStatus() throws Exception {
		var maria = findAttendantByName("Maria");

		mockMvc.perform(put("/api/attendants/{id}", maria.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Maria Oliveira",
					  "team": "LOANS",
					  "active": true
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(maria.getId()))
			.andExpect(jsonPath("$.name").value("Maria Oliveira"))
			.andExpect(jsonPath("$.team").value("LOANS"))
			.andExpect(jsonPath("$.active").value(true));

		mockMvc.perform(patch("/api/attendants/{id}/status", maria.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "active": false
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false))
			.andExpect(jsonPath("$.availableSlots").value(0));
	}

	@Test
	void shouldBlockAttendantDeactivationWhenThereIsInProgressAttendance() throws Exception {
		var joao = findAttendantByName("Joao");
		createAttendance(
			"Cliente Bloqueio",
			AttendanceStatus.IN_PROGRESS,
			AttendanceSubject.CARD_PROBLEM,
			TeamType.CARDS,
			joao,
			"Mensagem de bloqueio"
		);

		mockMvc.perform(patch("/api/attendants/{id}/status", joao.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "active": false
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Cannot deactivate attendant with attendances in progress"));
	}

	@Test
	void shouldCreateListAndFinishAttendance() throws Exception {
		var created = mockMvc.perform(post("/api/attendances")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "customerName": "Cliente API",
					  "message": "Cartao bloqueado apos compra online",
					  "subject": "CARD_PROBLEM"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.team").value("CARDS"))
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.attendantName").isString())
			.andReturn();

		var attendanceId = JsonTestUtils.readLong(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/attendances/{id}", attendanceId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(attendanceId))
			.andExpect(jsonPath("$.customerName").value("Cliente API"))
			.andExpect(jsonPath("$.message").value("Cartao bloqueado apos compra online"))
			.andExpect(jsonPath("$.attendantName").isString());

		mockMvc.perform(patch("/api/attendances/{id}/finish", attendanceId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FINISHED"))
			.andExpect(jsonPath("$.finishedAt").isNotEmpty());
	}

	@Test
	void shouldCreateAttendanceEndpointWithExpectedContract() throws Exception {
		mockMvc.perform(post("/api/attendances")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "customerName": "Cliente Contrato",
					  "message": "Preciso de orientacao sobre outro assunto",
					  "subject": "OTHER"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/attendances/")))
			.andExpect(jsonPath("$.customerName").value("Cliente Contrato"))
			.andExpect(jsonPath("$.message").value("Preciso de orientacao sobre outro assunto"))
			.andExpect(jsonPath("$.subject").value("OTHER"))
			.andExpect(jsonPath("$.team").value("OTHERS"))
			.andExpect(jsonPath("$.status").exists())
			.andExpect(jsonPath("$.attendantName").isString())
			.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void shouldFinishAttendanceEndpointAndReturnFinishedPayload() throws Exception {
		var joao = findAttendantByName("Joao");
		var inProgress = createAttendance(
			"Cliente Finalizacao",
			AttendanceStatus.IN_PROGRESS,
			AttendanceSubject.CARD_PROBLEM,
			TeamType.CARDS,
			joao,
			"Mensagem para finalizar"
		);

		mockMvc.perform(patch("/api/attendances/{id}/finish", inProgress.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(inProgress.getId()))
			.andExpect(jsonPath("$.status").value("FINISHED"))
			.andExpect(jsonPath("$.attendantId").value(joao.getId()))
			.andExpect(jsonPath("$.attendantName").value(joao.getName()))
			.andExpect(jsonPath("$.message").value("Mensagem para finalizar"))
			.andExpect(jsonPath("$.finishedAt").isNotEmpty());
	}

	@Test
	void shouldCreateAttendanceAsWaitingWhenTeamHasNoCapacity() throws Exception {
		var joao = findAttendantByName("Joao");
		var carla = findAttendantByName("Carla");
		fillCapacity(joao);
		fillCapacity(carla);

		mockMvc.perform(post("/api/attendances")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "customerName": "Cliente Sem Capacidade",
					  "message": "Mensagem em fila",
					  "subject": "CARD_PROBLEM"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("WAITING"))
			.andExpect(jsonPath("$.attendantId").doesNotExist())
			.andExpect(jsonPath("$.attendantName").doesNotExist());
	}

	@Test
	void shouldReturnDashboardSummary() throws Exception {
		var joao = findAttendantByName("Joao");
		createAttendance("Em andamento", AttendanceStatus.IN_PROGRESS, AttendanceSubject.CARD_PROBLEM, TeamType.CARDS, joao, "Cliente aguardando cartao");
		createAttendance("Na fila", AttendanceStatus.WAITING, AttendanceSubject.LOAN_REQUEST, TeamType.LOANS, null, "Cliente aguardando emprestimo");

		mockMvc.perform(get("/api/dashboard"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalAttendances").value(2))
			.andExpect(jsonPath("$.waiting").value(1))
			.andExpect(jsonPath("$.inProgress").value(1))
			.andExpect(jsonPath("$.averageQueueTimeMinutes").exists())
			.andExpect(jsonPath("$.averageServiceTimeMinutes").exists())
			.andExpect(jsonPath("$.teams[?(@.team=='CARDS')].inProgress").value(org.hamcrest.Matchers.contains(1)))
			.andExpect(jsonPath("$.teams[?(@.team=='CARDS')].averageServiceTimeMinutes").exists())
			.andExpect(jsonPath("$.queue[0].customerName").value("Na fila"))
			.andExpect(jsonPath("$.inProgressAttendances[0].customerName").value("Em andamento"))
			.andExpect(jsonPath("$.inProgressAttendances[0].attendantName").value("Joao"))
			.andExpect(jsonPath("$.inProgressAttendances[0].startedAt").isNotEmpty());
	}

	@Test
	void shouldOpenDashboardEventStream() throws Exception {
		mockMvc.perform(get("/api/dashboard/events"))
			.andExpect(request().asyncStarted())
			.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/event-stream")));
	}

	@Test
	void shouldReturnValidationErrorForInvalidPayload() throws Exception {
		mockMvc.perform(post("/api/attendances")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "customerName": "",
					  "message": "   ",
					  "subject": null
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Invalid request payload"))
			.andExpect(jsonPath("$.details").isArray());
	}

	@Test
	void shouldReturnNotFoundForMissingResource() throws Exception {
		mockMvc.perform(get("/api/attendances/999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Attendance not found: 999999"));
	}

	@Test
	void shouldReturnBusinessErrorWhenFinishingNonInProgressAttendance() throws Exception {
		var waiting = createAttendance("Ainda na fila", AttendanceStatus.WAITING, AttendanceSubject.OTHER, TeamType.OTHERS, null, "Mensagem de fila");

		mockMvc.perform(patch("/api/attendances/{id}/finish", waiting.getId()))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.message").value("Only in-progress attendances can be finished"));
	}

	@Test
	void shouldRedistributeOldestWaitingAttendanceWhenFinishing() throws Exception {
		var joao = findAttendantByName("Joao");
		createAttendance(
			"Capacidade Joao 1",
			AttendanceStatus.IN_PROGRESS,
			AttendanceSubject.CARD_PROBLEM,
			TeamType.CARDS,
			joao,
			"Mensagem em andamento 1"
		);
		createAttendance(
			"Capacidade Joao 2",
			AttendanceStatus.IN_PROGRESS,
			AttendanceSubject.CARD_PROBLEM,
			TeamType.CARDS,
			joao,
			"Mensagem em andamento 2"
		);
		var inProgress = createAttendance(
			"Em atendimento",
			AttendanceStatus.IN_PROGRESS,
			AttendanceSubject.CARD_PROBLEM,
			TeamType.CARDS,
			joao,
			"Mensagem em andamento 3"
		);
		var waiting = createAttendance(
			"Cliente em fila",
			AttendanceStatus.WAITING,
			AttendanceSubject.CARD_PROBLEM,
			TeamType.CARDS,
			null,
			"Mensagem em fila"
		);

		mockMvc.perform(patch("/api/attendances/{id}/finish", inProgress.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FINISHED"));

		mockMvc.perform(get("/api/attendances/{id}", waiting.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.attendantId").isNumber())
			.andExpect(jsonPath("$.attendantName").isString())
			.andExpect(jsonPath("$.startedAt").isNotEmpty());
	}

	private void fillCapacity(Attendant attendant) {
		createAttendance(attendant.getName() + " Cliente 1", AttendanceStatus.IN_PROGRESS, resolveSubject(attendant.getTeam()), attendant.getTeam(), attendant, "Mensagem 1");
		createAttendance(attendant.getName() + " Cliente 2", AttendanceStatus.IN_PROGRESS, resolveSubject(attendant.getTeam()), attendant.getTeam(), attendant, "Mensagem 2");
		createAttendance(attendant.getName() + " Cliente 3", AttendanceStatus.IN_PROGRESS, resolveSubject(attendant.getTeam()), attendant.getTeam(), attendant, "Mensagem 3");
	}

	private Attendant findAttendantByName(String name) {
		return attendantRepository.findAll().stream()
			.filter(attendant -> attendant.getName().equals(name))
			.findFirst()
			.orElseThrow();
	}

	private Attendance createAttendance(
		String customerName,
		AttendanceStatus status,
		AttendanceSubject subject,
		TeamType team,
		Attendant attendant,
		String message
	) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setMessage(message);
		attendance.setStatus(status);
		attendance.setSubject(subject);
		attendance.setTeam(team);
		attendance.setAttendant(attendant);
		if (status == AttendanceStatus.IN_PROGRESS) {
			attendance.setStartedAt(java.time.OffsetDateTime.now().minusMinutes(5));
		}
		return attendanceRepository.save(attendance);
	}

	private AttendanceSubject resolveSubject(TeamType team) {
		return switch (team) {
			case CARDS -> AttendanceSubject.CARD_PROBLEM;
			case LOANS -> AttendanceSubject.LOAN_REQUEST;
			case OTHERS -> AttendanceSubject.OTHER;
		};
	}
}
