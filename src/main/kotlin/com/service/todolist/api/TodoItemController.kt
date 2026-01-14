package com.service.todolist.api

import com.service.todolist.service.TodoItemService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/items")
class TodoItemController(private val service: TodoItemService) {
	private val logger = LoggerFactory.getLogger(javaClass)
	@PostMapping
	fun createItem(@Valid @RequestBody request: CreateTodoItemRequest): ResponseEntity<TodoItemResponse> {
		val item = service.createItem(request)
		logger.info("Creating new todo item; id=${item.id}")
		return ResponseEntity.status(HttpStatus.CREATED).body(TodoItemResponse.from(item))
	}

	@PutMapping("/{id}/description")
	fun updateDescription(
		@PathVariable id: Long,
		@Valid @RequestBody request: UpdateDescriptionRequest,
	): TodoItemResponse {
		val item = service.updateDescriptionById(id, request)
		logger.info("Updating description of todo item; id=$id, description='${request.description}'")
		return TodoItemResponse.from(item)
	}

	@PutMapping("/{id}/done")
	fun markDone(@PathVariable id: Long): TodoItemResponse {
		val item = service.markDoneById(id)
		logger.info("Marking todo item as done; id=$id")
		return TodoItemResponse.from(item)
	}

	@PutMapping("/{id}/not-done")
	fun markNotDone(@PathVariable id: Long): TodoItemResponse {
		val item = service.markNotDoneById(id)
		logger.info("Marking todo item as not done; id=$id")
		return TodoItemResponse.from(item)
	}

	@GetMapping("/{id}")
	fun getItem(@PathVariable id: Long): TodoItemResponse {
		val item = service.findById(id)
		return TodoItemResponse.from(item)
	}

	@GetMapping
	fun listItems(@RequestParam(defaultValue = "false") includeAll: Boolean): List<TodoItemResponse> {
		return service.findAllOrOnlyDone(includeAll).map(TodoItemResponse::from)
	}
}