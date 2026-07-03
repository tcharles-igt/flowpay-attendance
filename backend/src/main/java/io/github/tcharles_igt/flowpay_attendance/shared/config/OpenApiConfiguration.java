package io.github.tcharles_igt.flowpay_attendance.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI flowpayAttendanceOpenApi() {
		return new OpenAPI().info(new Info()
			.title("FlowPay Attendance API")
			.description("API base do desafio tecnico FlowPay Attendance.")
			.version("v1"));
	}
}
