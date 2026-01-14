package com.service.todolist.config

import io.mockk.every
import io.mockk.mockk
import jakarta.validation.ClockProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@TestConfiguration
class ClockTestConfig {

	@Bean
	fun clockProvider(): TestClockProvider = TestClockProvider(Instant.parse("2024-01-01T00:00:00Z"))

	@Bean
	@Primary
	fun clock(testClockProvider: TestClockProvider): Clock {
		val clock = mockk<Clock>()
		every { clock.instant() } answers { testClockProvider.currentInstant }
		every { clock.getZone() } returns ZoneOffset.UTC
		every { clock.zone } returns ZoneOffset.UTC
		every { clock.withZone(any()) } returns clock
		return clock
	}
}

class TestClockProvider(
	var currentInstant: Instant,
): ClockProvider {
	fun setInstant(newInstant: Instant) {
		currentInstant = newInstant
	}

	override fun getClock(): Clock {
		return Clock.fixed(currentInstant, ZoneOffset.UTC)
	}
}