package com.service.todolist.config

import jakarta.validation.ClockProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import jakarta.validation.Configuration as ValidationConfiguration

@Configuration
class ValidationConfig {

	@Bean
	@Primary
	fun validator(clockProvider: ClockProvider): LocalValidatorFactoryBean =
		LocalValidatorFactoryBean().apply {
			setConfigurationInitializer { configuration: ValidationConfiguration<*> ->
				configuration.clockProvider(clockProvider)
			}
		}
}