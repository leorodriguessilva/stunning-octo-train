package com.service.todolist.service

import com.service.todolist.api.CreateTodoItemRequest
import com.service.todolist.api.UpdateDescriptionRequest
import com.service.todolist.model.TodoStatus
import com.service.todolist.repository.TodoItemRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DataJpaTest
@Import(TodoItemService::class, TodoItemServiceTest.ClockTestConfig::class)
class TodoItemServiceTest {
	@Autowired
	private lateinit var service: TodoItemService

	@Autowired
	private lateinit var repository: TodoItemRepository

	@Autowired
	private lateinit var clockHolder: TestClockHolder

	@AfterEach
	fun tearDown() {
		repository.deleteAll()
	}

	@Test
	fun `given new item when created then it is stored`() {
		val request = CreateTodoItemRequest(
			description = "Write tests",
			dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
		)

		val saved = service.createItem(request)

		val items = repository.findAll()
		assertThat(items).hasSize(1)
		assertThat(saved.id).isNotNull
		assertThat(saved.description).isEqualTo("Write tests")
		assertThat(saved.status).isEqualTo(TodoStatus.NOT_DONE)
		assertThat(saved.dueDatetime).isEqualTo(request.dueDatetime)
	}

	@Test
	fun `given existing item when description updated then it changes`() {
		val item = service.createItem(
			CreateTodoItemRequest(
				description = "Initial",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)

		val updated = service.updateDescription(
			item.id ?: 0,
			UpdateDescriptionRequest("Updated"),
		)

		assertThat(updated.description).isEqualTo("Updated")
	}

	@Test
	fun `given not done item when marked done then status is done`() {
		val item = service.createItem(
			CreateTodoItemRequest(
				description = "Pay bills",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)

		val updated = service.markDone(item.id ?: 0)

		assertThat(updated.status).isEqualTo(TodoStatus.DONE)
		assertThat(updated.doneDatetime).isNotNull
	}

	@Test
	fun `given done item when marked not done then status is not done`() {
		val item = service.createItem(
			CreateTodoItemRequest(
				description = "Call supplier",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)

		service.markDone(item.id ?: 0)

		val updated = service.markNotDone(item.id ?: 0)

		assertThat(updated.status).isEqualTo(TodoStatus.NOT_DONE)
		assertThat(updated.doneDatetime).isNull()
	}

	@Test
	fun `given item exists when fetched by id then details are returned`() {
		val item = service.createItem(
			CreateTodoItemRequest(
				description = "Prepare slides",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)

		val fetched = service.getItem(item.id ?: 0)

		assertThat(fetched.id).isEqualTo(item.id)
		assertThat(fetched.description).isEqualTo("Prepare slides")
	}

	@Test
	fun `given item does not exists when fetched by id then throws an error`() {
		val exception = assertThrows<ResponseStatusException> {
			service.getItem(9999)
		}

		assertThat(exception.statusCode.value()).isEqualTo(404)
	}

	@Test
	fun `given mixed items when listing not done then only not done returned`() {
		service.createItem(
			CreateTodoItemRequest(
				description = "Not done",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)

		val done = service.createItem(
			CreateTodoItemRequest(
				description = "Done",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)
		service.markDone(done.id ?: 0)

		service.createItem(
			CreateTodoItemRequest(
				description = "Past due",
				dueDatetime = clockHolder.currentInstant.minusSeconds(60),
			),
		)

		val items = service.listItems(includeAll = false)

		assertThat(items).hasSize(1)
		assertThat(items.first().status).isEqualTo(TodoStatus.NOT_DONE)
	}

	@Test
	fun `given mixed items when listing all then all returned`() {
		service.createItem(
			CreateTodoItemRequest(
				description = "Not done",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)

		val done = service.createItem(
			CreateTodoItemRequest(
				description = "Done",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)
		service.markDone(done.id ?: 0)

		service.createItem(
			CreateTodoItemRequest(
				description = "Past due",
				dueDatetime = clockHolder.currentInstant.minusSeconds(60),
			),
		)

		val items = service.listItems(includeAll = true)

		assertThat(items).hasSize(3)
	}

	@Test
	fun `given item past due when status check runs then status becomes past due`() {
		val dueDatetime = clockHolder.currentInstant.plusSeconds(30)
		val item = service.createItem(
			CreateTodoItemRequest(
				description = "Submit report",
				dueDatetime = dueDatetime,
			),
		)

		clockHolder.setInstant(dueDatetime.plusSeconds(30))
		service.updatePastDue()

		val refreshed = repository.findById(item.id ?: 0).orElseThrow()
		assertThat(refreshed.status).isEqualTo(TodoStatus.PAST_DUE)
	}

	@Test
	fun `given past due item when updating then throws an error`() {
		val item = service.createItem(
			CreateTodoItemRequest(
				description = "Expired task",
				dueDatetime = clockHolder.currentInstant.minusSeconds(30),
			),
		)

		val exception = assertThrows<PastDueItemException> {
			service.updateDescription(
				item.id ?: 0,
				UpdateDescriptionRequest("New description"),
			)
		}

		assertThat(exception.message).contains("past due")
	}

	@TestConfiguration
	class ClockTestConfig {
		@Bean
		@Primary
		fun testClockHolder(): TestClockHolder = TestClockHolder(Instant.parse("2024-01-01T00:00:00Z"))

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
}

class TestClockHolder(
	var currentInstant: Instant,
) {
	fun setInstant(instant: Instant) {
		currentInstant = instant
	}
}
