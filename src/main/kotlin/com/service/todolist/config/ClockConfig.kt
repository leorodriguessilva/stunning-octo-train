package com.service.todolist.config

import jakarta.validation.ClockProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {

	class TodoClockProvider(val clockReference: Clock = Clock.systemUTC()): ClockProvider {
		override fun getClock(): Clock {
			return clockReference
		}
	}

	@Bean
	fun clockProvider(): ClockProvider = TodoClockProvider()

	@Bean
	fun clock(clockProvider: ClockProvider): Clock = clockProvider.clock
}