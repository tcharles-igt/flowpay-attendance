package io.github.tcharles_igt.flowpay_attendance.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
			.andExpect(jsonPath("$.active").value(true));

		mockMvc.perform(get("/api/attendants"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.name=='Patricia')]").exists());
	}

	@Test
	void shouldCreateListAndFinishAttendance() throws Exception {
		var created = mockMvc.perform(post("/api/attendances")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "customerName": "Cliente API",
					  "subject": "CARD_PROBLEM"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.team").value("CARDS"))
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andReturn();

		var attendanceId = JsonTestUtils.readLong(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/attendances/{id}", attendanceId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(attendanceId))
			.andExpect(jsonPath("$.customerName").value("Cliente API"));

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
					  "subject": "OTHER"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/attendances/")))
			.andExpect(jsonPath("$.customerName").value("Cliente Contrato"))
			.andExpect(jsonPath("$.subject").value("OTHER"))
			.andExpect(jsonPath("$.team").value("OTHERS"))
			.andExpect(jsonPath("$.status").exists())
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
			joao
		);

		mockMvc.perform(patch("/api/attendances/{id}/finish", inProgress.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(inProgress.getId()))
			.andExpect(jsonPath("$.status").value("FINISHED"))
			.andExpect(jsonPath("$.attendantId").value(joao.getId()))
			.andExpect(jsonPath("$.finishedAt").isNotEmpty());
	}

	@Test
	void shouldReturnDashboardSummary() throws Exception {
		var joao = findAttendantByName("Joao");
		createAttendance("Em andamento", AttendanceStatus.IN_PROGRESS, AttendanceSubject.CARD_PROBLEM, TeamType.CARDS, joao);
		createAttendance("Na fila", AttendanceStatus.WAITING, AttendanceSubject.LOAN_REQUEST, TeamType.LOANS, null);

		mockMvc.perform(get("/api/dashboard"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalAttendances").value(2))
			.andExpect(jsonPath("$.waiting").value(1))
			.andExpect(jsonPath("$.inProgress").value(1))
			.andExpect(jsonPath("$.teams[?(@.team=='CARDS')].inProgress").value(org.hamcrest.Matchers.contains(1)))
			.andExpect(jsonPath("$.queue[0].customerName").value("Na fila"))
			.andExpect(jsonPath("$.inProgressAttendances[0].customerName").value("Em andamento"))
			.andExpect(jsonPath("$.inProgressAttendances[0].attendantName").value("Joao"))
			.andExpect(jsonPath("$.inProgressAttendances[0].startedAt").isNotEmpty());
	}

	@Test
	void shouldReturnValidationErrorForInvalidPayload() throws Exception {
		mockMvc.perform(post("/api/attendances")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "customerName": "",
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
		var waiting = createAttendance("Ainda na fila", AttendanceStatus.WAITING, AttendanceSubject.OTHER, TeamType.OTHERS, null);

		mockMvc.perform(patch("/api/attendances/{id}/finish", waiting.getId()))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.message").value("Only in-progress attendances can be finished"));
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
		Attendant attendant
	) {
		var attendance = new Attendance();
		attendance.setCustomerName(customerName);
		attendance.setStatus(status);
		attendance.setSubject(subject);
		attendance.setTeam(team);
		attendance.setAttendant(attendant);
		if (status == AttendanceStatus.IN_PROGRESS) {
			attendance.setStartedAt(java.time.OffsetDateTime.now().minusMinutes(5));
		}
		return attendanceRepository.save(attendance);
	}
}
