package com.service.todolist.api

import com.service.todolist.model.TodoItem
import com.service.todolist.model.TodoStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CreateTodoItemRequest(
	@field:NotBlank
	val description: String,
	@field:NotNull
	val dueDatetime: Instant,
)

data class UpdateDescriptionRequest(
	@field:NotBlank
	val description: String,
)

data class TodoItemResponse(
	val id: Long,
	val description: String,
	val status: TodoStatus,
	val creationDatetime: Instant,
	val dueDatetime: Instant,
	val doneDatetime: Instant?,
) {
	companion object {
		fun from(item: TodoItem): TodoItemResponse {
			return TodoItemResponse(
				id = item.id ?: 0,
				description = item.description,
				status = item.status,
				creationDatetime = item.creationDatetime,
				dueDatetime = item.dueDatetime,
				doneDatetime = item.doneDatetime,
			)
		}
	}
}