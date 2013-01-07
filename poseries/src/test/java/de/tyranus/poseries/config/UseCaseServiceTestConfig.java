package de.tyranus.poseries.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tyranus.poseries.usecase.UseCase;
import de.tyranus.poseries.usecase.intern.UseCaseImpl;

@Configuration
public class UseCaseServiceTestConfig {
	@Bean
	public UseCase useCaseService() {
		return new UseCaseImpl(1);
	}
}
