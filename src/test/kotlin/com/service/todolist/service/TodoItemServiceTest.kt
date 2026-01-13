package com.service.todolist.service

import com.service.todolist.api.CreateTodoItemRequest
import com.service.todolist.api.UpdateDescriptionRequest
import com.service.todolist.model.TodoItem
import com.service.todolist.model.TodoStatus
import com.service.todolist.repository.TodoItemRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Sort
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DataJpaTest
@Import(TodoItemService::class, TodoItemServiceTest.ClockTestConfig::class)
class TodoItemServiceTest {

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

	class TestClockHolder(
		var currentInstant: Instant,
	) {
		fun setInstant(newInstant: Instant) {
			currentInstant = newInstant
		}
	}

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

		val updated = service.updateDescriptionById(
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

		val updated = service.markDoneById(item.id ?: 0)

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

		service.markDoneById(item.id ?: 0)

		val updated = service.markNotDoneById(item.id ?: 0)

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

		val fetched = service.findById(item.id ?: 0)

		assertThat(fetched.id).isEqualTo(item.id)
		assertThat(fetched.description).isEqualTo("Prepare slides")
	}

	@Test
	fun `given item exists when fetched by id and past due then details are returned with past due status`() {
		val item = service.createItem(
			CreateTodoItemRequest(
				description = "Prepare slides",
				dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
			),
		)

		clockHolder.setInstant(clockHolder.currentInstant.plusSeconds(7200))
		val fetched = service.findById(item.id ?: 0)

		assertThat(fetched.id).isEqualTo(item.id)
		assertThat(fetched.status).isEqualTo(TodoStatus.PAST_DUE)
	}

	@Test
	fun `given item does not exists when fetched by id then throws an error`() {
		val exception = assertThrows<ResponseStatusException> {
			service.findById(9999)
		}

		assertThat(exception.statusCode.value()).isEqualTo(404)
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
			service.updateDescriptionById(
				item.id ?: 0,
				UpdateDescriptionRequest("New description"),
			)
		}

		assertThat(exception.message).contains("past due")
	}

	@Nested
	internal inner class ListAllTodosTest {

		@BeforeEach
		fun setup() {
			repository.save(
				TodoItem(
					description = "Past due task",
					status = TodoStatus.NOT_DONE,
					creationDatetime = clockHolder.currentInstant.minusSeconds(7200),
					dueDatetime = clockHolder.currentInstant.minusSeconds(3600),
				),
			)
			repository.save(
				TodoItem(
					description = "Not due task",
					status = TodoStatus.NOT_DONE,
					creationDatetime = clockHolder.currentInstant.minusSeconds(7200),
					dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
				),
			)
			repository.save(
				TodoItem(
					description = "Done before due time task",
					status = TodoStatus.DONE,
					creationDatetime = clockHolder.currentInstant.minusSeconds(7200),
					dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
				),
			)
		}

		@Test
		fun `given mixed items when listing not done then only not done returned`() {
			val items = service.findAllOrOnlyDone(false)

			assertThat(items).hasSize(1)
			assertThat(items.first().status).isEqualTo(TodoStatus.NOT_DONE)
		}

		@Test
		fun `given mixed items when listing all then all returned`() {
			val items = service.findAllOrOnlyDone(true)
			assertThat(items).hasSize(3)
		}

		@Test
		fun `given listing all items when one is past due then it should be returned with updated status `() {
			val items = service.findAllOrOnlyDone(true)
			assertThat(items).hasSize(3)
			assertThat(items).anyMatch { it.status == TodoStatus.PAST_DUE }
		}
	}

	@Nested
	internal inner class MarkAsPastDueCronJobTest {

		@BeforeEach
		fun setup() {
			repository.save(
				TodoItem(
					description = "Past due task",
					status = TodoStatus.NOT_DONE,
					creationDatetime = clockHolder.currentInstant.minusSeconds(7200),
					dueDatetime = clockHolder.currentInstant.minusSeconds(3600),
				),
			)
			repository.save(
				TodoItem(
					description = "Not due task",
					status = TodoStatus.NOT_DONE,
					creationDatetime = clockHolder.currentInstant.minusSeconds(7200),
					dueDatetime = clockHolder.currentInstant.plusSeconds(3600),
				),
			)
			repository.save(
				TodoItem(
					description = "Done before due time task",
					status = TodoStatus.DONE,
					creationDatetime = clockHolder.currentInstant.minusSeconds(7200),
					dueDatetime = clockHolder.currentInstant.minusSeconds(3600),
				),
			)
		}

		@Test
		fun `given item past due when status check runs then status becomes past due`() {
			service.updatePastDueTodos()

			val allPastDue = repository.findByStatus(TodoStatus.PAST_DUE, Sort.unsorted())
			assertThat(allPastDue.size).isEqualTo(1)
			val expectedPastDue = allPastDue.first()
			assertThat(expectedPastDue).isNotNull
			assertThat(expectedPastDue.status).isEqualTo(TodoStatus.PAST_DUE)
		}

	}
}
