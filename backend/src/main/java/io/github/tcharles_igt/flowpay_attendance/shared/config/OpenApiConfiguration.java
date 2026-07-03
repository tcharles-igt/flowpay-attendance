package io.github.tcharles_igt.flowpay_attendance.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI flowpayAttendanceOpenApi() {
		return new OpenAPI().info(new Info()
			.title("FlowPay Attendance API")
			.description(
				"API do desafio FlowPay Attendance. A plataforma distribui atendimentos por time, respeita a capacidade "
					+ "de cada atendente, mantem fila por especialidade e expõe um dashboard operacional para acompanhamento."
			)
			.version("v1")
			.license(new License().name("Uso tecnico para avaliacao do desafio")));
	}
}
