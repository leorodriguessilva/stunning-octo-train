package com.service.todolist.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.service.todolist.model.TodoItem
import com.service.todolist.model.TodoStatus
import com.service.todolist.service.PastDueItemException
import com.service.todolist.service.TodoItemService
import jakarta.persistence.EntityNotFoundException
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import java.time.Instant

@WebMvcTest(TodoItemController::class)
class TodoItemControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@MockBean
	private lateinit var service: TodoItemService

	@Test
	fun `given create request when posting item then returns payload`() {
		val dueDatetime = Instant.parse("2024-01-01T12:00:00Z")
		val request = CreateTodoItemRequest(
			description = "Write documentation",
			dueDatetime = dueDatetime,
		)
		val item = TodoItem(
			id = 1,
			description = request.description,
			status = TodoStatus.NOT_DONE,
			creationDatetime = Instant.parse("2024-01-01T10:00:00Z"),
			dueDatetime = dueDatetime,
			doneDatetime = null,
		)
		given(service.createItem(request)).willReturn(item)

		mockMvc.post("/items") {
			contentType = MediaType.APPLICATION_JSON
			content = objectMapper.writeValueAsString(request)
		}
			.andExpect {
				status { isCreated() }
				jsonPath("$", hasKey("id"))
				jsonPath("$", hasKey("description"))
				jsonPath("$", hasKey("status"))
				jsonPath("$", hasKey("creationDatetime"))
				jsonPath("$", hasKey("dueDatetime"))
			}
	}

	@Test
	fun `given existing item when fetching by id then returns payload`() {
		val item = TodoItem(
			id = 42,
			description = "Submit report",
			status = TodoStatus.NOT_DONE,
			creationDatetime = Instant.parse("2024-01-01T09:00:00Z"),
			dueDatetime = Instant.parse("2024-01-01T12:00:00Z"),
			doneDatetime = null,
		)
		given(service.findById(42)).willReturn(item)

		mockMvc.get("/items/42")
			.andExpect {
				status { isOk() }
				jsonPath("$", hasKey("id"))
				jsonPath("$", hasKey("description"))
				jsonPath("$", hasKey("status"))
				jsonPath("$", hasKey("creationDatetime"))
				jsonPath("$", hasKey("dueDatetime"))
			}
	}

	@Test
	fun `given missing item when fetching by id then returns not found`() {
		given(service.findById(9999))
			.willThrow(EntityNotFoundException("Todo item 9999 not found"))
		mockMvc.get("/items/9999")
			.andExpect {
				status { isNotFound() }
			}
	}

	@Test
	fun `given existing item when marking done then returns payload`() {
		val item = TodoItem(
			id = 7,
			description = "Finalize contract",
			status = TodoStatus.DONE,
			creationDatetime = Instant.parse("2024-01-01T08:00:00Z"),
			dueDatetime = Instant.parse("2024-01-01T12:00:00Z"),
			doneDatetime = Instant.parse("2024-01-01T09:00:00Z"),
		)
		given(service.markDoneById(7)).willReturn(item)

		mockMvc.put("/items/7/done")
			.andExpect {
				status { isOk() }
				jsonPath("$", hasKey("id"))
				jsonPath("$", hasKey("description"))
				jsonPath("$", hasKey("status"))
				jsonPath("$", hasKey("creationDatetime"))
				jsonPath("$", hasKey("dueDatetime"))
				jsonPath("$", hasKey("doneDatetime"))
			}
	}

	@Test
	fun `given existing item when marking not done then returns payload`() {
		val item = TodoItem(
			id = 8,
			description = "Update backlog",
			status = TodoStatus.NOT_DONE,
			creationDatetime = Instant.parse("2024-01-01T08:00:00Z"),
			dueDatetime = Instant.parse("2024-01-01T12:00:00Z"),
			doneDatetime = null,
		)
		given(service.markNotDoneById(8)).willReturn(item)

		mockMvc.put("/items/8/not-done")
			.andExpect {
				status { isOk() }
				jsonPath("$", hasKey("id"))
				jsonPath("$", hasKey("description"))
				jsonPath("$", hasKey("status"))
				jsonPath("$", hasKey("creationDatetime"))
				jsonPath("$", hasKey("dueDatetime"))
			}
	}

	@Test
	fun `given update request when updating description then returns payload`() {
		val request = UpdateDescriptionRequest("Updated description")
		val item = TodoItem(
			id = 9,
			description = request.description,
			status = TodoStatus.NOT_DONE,
			creationDatetime = Instant.parse("2024-01-01T08:00:00Z"),
			dueDatetime = Instant.parse("2024-01-01T12:00:00Z"),
			doneDatetime = null,
		)
		given(service.updateDescriptionById(9, request)).willReturn(item)

		mockMvc.put("/items/9/description") {
			contentType = MediaType.APPLICATION_JSON
			content = objectMapper.writeValueAsString(request)
		}
			.andExpect {
				status { isOk() }
				jsonPath("$", hasKey("id"))
				jsonPath("$", hasKey("description"))
				jsonPath("$", hasKey("status"))
				jsonPath("$", hasKey("creationDatetime"))
				jsonPath("$", hasKey("dueDatetime"))
			}
	}

	@Test
	fun `given items when listing with includeAll then returns list`() {
		val items = listOf(
			TodoItem(
				id = 10,
				description = "Pay invoices",
				status = TodoStatus.NOT_DONE,
				creationDatetime = Instant.parse("2024-01-01T08:00:00Z"),
				dueDatetime = Instant.parse("2024-01-01T12:00:00Z"),
				doneDatetime = null,
			),
			TodoItem(
				id = 11,
				description = "Book venue",
				status = TodoStatus.NOT_DONE,
				creationDatetime = Instant.parse("2024-01-01T08:30:00Z"),
				dueDatetime = Instant.parse("2024-01-01T13:00:00Z"),
				doneDatetime = null,
			),
			TodoItem(
				id = 12,
				description = "Find keys",
				status = TodoStatus.DONE,
				creationDatetime = Instant.parse("2024-01-01T08:30:00Z"),
				dueDatetime = Instant.parse("2024-01-01T13:00:00Z"),
				doneDatetime = Instant.parse("2024-01-01T12:00:00Z"),
			),
			TodoItem(
				id = 12,
				description = "Forget to find keys",
				status = TodoStatus.PAST_DUE,
				creationDatetime = Instant.parse("2024-01-01T08:30:00Z"),
				dueDatetime = Instant.parse("2024-01-01T13:00:00Z"),
				doneDatetime = null,
			),
		)
		given(service.findAllOrOnlyDone(false))
			.willReturn(items.filter { it.status == TodoStatus.DONE })
		given(service.findAllOrOnlyDone(true)).willReturn(items)

		mockMvc.get("/items")
			.andExpect {
				status { isOk() }
				jsonPath("$", hasSize<Any>(1))
			}

		mockMvc.get("/items?includeAll=true")
			.andExpect {
				status { isOk() }
				jsonPath("$", hasSize<Any>(4))
			}
	}

	@Nested
	internal inner class ErrorResponses {
		@Test
		fun `given invalid create request when posting item then returns bad request`() {
			val request = CreateTodoItemRequest(
				description = "",
				dueDatetime = Instant.parse("2024-01-01T12:00:00Z"),
			)

			mockMvc.post("/items") {
				contentType = MediaType.APPLICATION_JSON
				content = objectMapper.writeValueAsString(request)
			}
				.andExpect {
					status { isBadRequest() }
					jsonPath("$", hasKey("error"))
				}
		}

		@Test
		fun `given invalid update request when updating description then returns bad request`() {
			val request = UpdateDescriptionRequest("")

			mockMvc.put("/items/9/description") {
				contentType = MediaType.APPLICATION_JSON
				content = objectMapper.writeValueAsString(request)
			}
				.andExpect {
					status { isBadRequest() }
					jsonPath("$", hasKey("error"))
				}
		}

		@Test
		fun `given past due item when marking done then returns conflict`() {
			given(service.markDoneById(7)).willThrow(PastDueItemException("Todo item 7 is past due"))

			mockMvc.put("/items/7/done")
				.andExpect {
					status { isConflict() }
					jsonPath("$", hasKey("error"))
				}
		}

		@Test
		fun `given past due item when marking not done then returns conflict`() {
			given(service.markNotDoneById(8)).willThrow(PastDueItemException("Todo item 8 is past due"))

			mockMvc.put("/items/8/not-done")
				.andExpect {
					status { isConflict() }
					jsonPath("$", hasKey("error"))
				}
		}

		@Test
		fun `given past due item when updating description then returns conflict`() {
			val request = UpdateDescriptionRequest("Updated description")
			given(service.updateDescriptionById(9, request))
				.willThrow(PastDueItemException("Todo item 9 is past due"))

			mockMvc.put("/items/9/description") {
				contentType = MediaType.APPLICATION_JSON
				content = objectMapper.writeValueAsString(request)
			}
				.andExpect {
					status { isConflict() }
					jsonPath("$", hasKey("error"))
				}
		}

		@Test
		fun `given missing item when marking done then returns not found`() {
			given(service.markDoneById(7)).willThrow(EntityNotFoundException("Todo item 7 not found"))

			mockMvc.put("/items/7/done")
				.andExpect {
					status { isNotFound() }
					jsonPath("$", hasKey("error"))
				}
		}

		@Test
		fun `given missing item when marking not done then returns not found`() {
			given(service.markNotDoneById(8)).willThrow(EntityNotFoundException("Todo item 8 not found"))

			mockMvc.put("/items/8/not-done")
				.andExpect {
					status { isNotFound() }
					jsonPath("$", hasKey("error"))
				}
		}

		@Test
		fun `given missing item when updating description then returns not found`() {
			val request = UpdateDescriptionRequest("Updated description")
			given(service.updateDescriptionById(9, request))
				.willThrow(EntityNotFoundException("Todo item 9 not found"))

			mockMvc.put("/items/9/description") {
				contentType = MediaType.APPLICATION_JSON
				content = objectMapper.writeValueAsString(request)
			}
				.andExpect {
					status { isNotFound() }
					jsonPath("$", hasKey("error"))
				}
		}

		@Test
		fun `given unexpected error when posting item then returns server error`() {
			val request = CreateTodoItemRequest(
				description = "Write documentation",
				dueDatetime = Instant.parse("2024-01-01T12:00:00Z"),
			)
			given(service.createItem(request))
				.willThrow(IllegalStateException("Unexpected error"))

			mockMvc.post("/items") {
				contentType = MediaType.APPLICATION_JSON
				content = objectMapper.writeValueAsString(request)
			}
				.andExpect {
					status { isInternalServerError() }
					jsonPath("$", hasKey("error"))
				}
		}
	}
}