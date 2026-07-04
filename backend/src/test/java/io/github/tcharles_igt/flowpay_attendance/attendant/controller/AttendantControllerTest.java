package io.github.tcharles_igt.flowpay_attendance.attendant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantResponse;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantStatusRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.dto.AttendantUpdateRequest;
import io.github.tcharles_igt.flowpay_attendance.attendant.service.AttendantService;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;

@ExtendWith(MockitoExtension.class)
class AttendantControllerTest {

	@Mock
	private AttendantService attendantService;

	@Test
	void shouldDelegateCreateRequestAndReturnCreatedResponse() {
		var request = new AttendantRequest("Patricia", TeamType.LOANS, true);
		var response = response(10L, "Patricia", TeamType.LOANS, true, 0, 3);
		when(attendantService.create(request)).thenReturn(response);
		var controller = new AttendantController(attendantService);
		var servletRequest = new MockHttpServletRequest("POST", "/api/attendants");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

		try {
			var result = controller.create(request);

			assertThat(result.getStatusCode().value()).isEqualTo(201);
			assertThat(result.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost/api/attendants/10"));
			assertThat(result.getBody()).isSameAs(response);
			verify(attendantService).create(request);
		} finally {
			RequestContextHolder.resetRequestAttributes();
		}
	}

	@Test
	void shouldDelegateListRequest() {
		var response = List.of(response(1L, "Ana", TeamType.OTHERS, true, 1, 2));
		when(attendantService.findAll()).thenReturn(response);
		var controller = new AttendantController(attendantService);

		var result = controller.findAll();

		assertThat(result).isSameAs(response);
		verify(attendantService).findAll();
	}

	@Test
	void shouldDelegateFindByIdRequest() {
		var response = response(2L, "Carla", TeamType.CARDS, true, 2, 1);
		when(attendantService.findById(2L)).thenReturn(response);
		var controller = new AttendantController(attendantService);

		var result = controller.findById(2L);

		assertThat(result).isSameAs(response);
		verify(attendantService).findById(2L);
	}

	@Test
	void shouldDelegateUpdateRequest() {
		var request = new AttendantUpdateRequest("Carla Souza", TeamType.CARDS, false);
		var response = response(2L, "Carla Souza", TeamType.CARDS, false, 0, 0);
		when(attendantService.update(2L, request)).thenReturn(response);
		var controller = new AttendantController(attendantService);

		var result = controller.update(2L, request);

		assertThat(result).isSameAs(response);
		verify(attendantService).update(2L, request);
	}

	@Test
	void shouldDelegateStatusUpdateRequest() {
		var request = new AttendantStatusRequest(false);
		var response = response(2L, "Carla", TeamType.CARDS, false, 0, 0);
		when(attendantService.updateStatus(2L, request)).thenReturn(response);
		var controller = new AttendantController(attendantService);

		var result = controller.updateStatus(2L, request);

		assertThat(result).isSameAs(response);
		verify(attendantService).updateStatus(2L, request);
	}

	private AttendantResponse response(
		Long id,
		String name,
		TeamType team,
		boolean active,
		long activeAttendances,
		long availableSlots
	) {
		return new AttendantResponse(
			id,
			name,
			team,
			active,
			activeAttendances,
			availableSlots,
			OffsetDateTime.now().minusHours(1),
			OffsetDateTime.now()
		);
	}
}
