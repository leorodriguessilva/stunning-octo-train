package com.service.todolist.service

import com.service.todolist.api.CreateTodoItemRequest
import com.service.todolist.api.UpdateDescriptionRequest
import com.service.todolist.model.TodoItem
import com.service.todolist.model.TodoStatus
import com.service.todolist.model.throwPastDueException
import com.service.todolist.model.trySetPastDue
import com.service.todolist.repository.TodoItemRepository
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Instant

@Service
class TodoItemService(
	private val repository: TodoItemRepository,
	private val clock: Clock,
) {
	@Transactional
	fun createItem(request: CreateTodoItemRequest): TodoItem {
		val now = Instant.now(clock)
		val item = TodoItem(
			description = request.description,
			status = TodoStatus.NOT_DONE,
			creationDatetime = now,
			dueDatetime = request.dueDatetime,
			doneDatetime = null,
		)
		item.trySetPastDue(Instant.now(clock))
		val saved = repository.save(item)
		return repository.findById(saved.id ?: 0).orElse(saved)
	}

	@Transactional
	fun updateDescription(id: Long, request: UpdateDescriptionRequest): TodoItem {
		val item = getItemOrThrow(id)
		val isPastDue = item.trySetPastDue(Instant.now(clock))
		if (isPastDue) {
			item.throwPastDueException()
		}
		item.description = request.description
		return repository.save(item)
	}

	@Transactional
	fun markDone(id: Long): TodoItem {
		val now = Instant.now(clock)
		val item = getItemOrThrow(id)
		val isPastDue = item.trySetPastDue(Instant.now(clock))
		if (isPastDue) {
			item.throwPastDueException()
		}
		item.status = TodoStatus.DONE
		item.doneDatetime = now
		return repository.save(item)
	}

	@Transactional
	fun markNotDone(id: Long): TodoItem {
		updatePastDue()
		val item = getItemOrThrow(id)
		val isPastDue = item.trySetPastDue(Instant.now(clock))
		if (isPastDue) {
			item.throwPastDueException()
		}
		item.status = TodoStatus.NOT_DONE
		item.doneDatetime = null
		val saved = repository.save(item)
		return repository.findById(saved.id ?: 0).orElse(saved)
	}

	@Transactional
	fun getItem(id: Long): TodoItem {
		updatePastDue()
		val item = getItemOrThrow(id)
		val isPastDue = item.trySetPastDue(Instant.now(clock))
		if (isPastDue) {
			return repository.save(item)
		}
		return item
	}

	@Transactional
	fun listItems(includeAll: Boolean): List<TodoItem> {
		updatePastDue()
		val sort = Sort.by("creationDatetime").ascending()
		return if (includeAll) {
			repository.findAll(sort)
		} else {
			repository.findByStatus(TodoStatus.NOT_DONE, sort)
		}
	}

	@Transactional
	@Scheduled(fixedDelayString = "PT1M")
	fun updatePastDue() {
		repository.markPastDue(Instant.now(clock))
	}

	private fun getItemOrThrow(id: Long): TodoItem {
		return repository.findById(id).orElseThrow {
			ResponseStatusException(HttpStatus.NOT_FOUND, "Todo item $id not found")
		}
	}
}