package io.github.tcharles_igt.flowpay_attendance.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

	@Bean
	Clock applicationClock() {
		return Clock.systemUTC();
	}
}
