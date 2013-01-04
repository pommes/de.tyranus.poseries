package de.tyranus.poseries.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tyranus.poseries.usecase.UseCaseService;
import de.tyranus.poseries.usecase.intern.UseCaseServiceImpl;

@Configuration
public class UseCaseServiceTestConfig {
	@Bean
	public UseCaseService useCaseService() {
		return new UseCaseServiceImpl(1);
	}
}
