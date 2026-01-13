package com.service.todolist.api

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