package com.service.todolist.config

import io.mockk.every
import io.mockk.mockk
import jakarta.validation.ClockProvider
import jakarta.validation.Configuration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@TestConfiguration
class ClockTestConfig {
	@Bean
	@Primary
	fun testClockHolder(): TestClockHolder = TestClockHolder(Instant.parse("2024-01-01T00:00:00Z"))


	@Bean
	@Primary
	fun validator(clockHolder: TestClockHolder): LocalValidatorFactoryBean =
		LocalValidatorFactoryBean().apply {
			setConfigurationInitializer { configuration: Configuration<*> ->
				configuration.clockProvider(TestClockProvider(clockHolder))
			}
		}

	@Bean
	@Primary
	fun clock(clockHolder: TestClockHolder): Clock {
		val clock = mockk<Clock>()
		every { clock.instant() } answers { clockHolder.currentInstant }
		every { clock.getZone() } returns ZoneOffset.UTC
		every { clock.zone } returns ZoneOffset.UTC
		every { clock.withZone(any()) } returns clock
		return clock
	}
}

class TestClockHolder(
	var currentInstant: Instant,
) {
	fun setInstant(newInstant: Instant) {
		currentInstant = newInstant
	}
}

class TestClockProvider(
	private val clockHolder: TestClockHolder,
) : ClockProvider {
	override fun getClock(): Clock {
		return Clock.fixed(clockHolder.currentInstant, ZoneOffset.UTC)
	}
}