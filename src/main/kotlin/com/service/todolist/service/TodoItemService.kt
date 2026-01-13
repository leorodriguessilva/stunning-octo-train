package com.service.todolist.service

import com.service.todolist.api.CreateTodoItemRequest
import com.service.todolist.api.UpdateDescriptionRequest
import com.service.todolist.model.TodoItem
import com.service.todolist.model.TodoStatus
import com.service.todolist.model.assertStillDoable
import com.service.todolist.model.isNotDoneNowDue
import com.service.todolist.repository.TodoItemRepository
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
		val isTodoNotDoneNowDue = item.isNotDoneNowDue(Instant.now(clock))
		if (isTodoNotDoneNowDue) {
			item.status = TodoStatus.PAST_DUE
		}
		val saved = repository.save(item)
		return repository.findById(saved.id ?: 0).orElse(saved)
	}

	@Transactional
	fun updateDescriptionById(id: Long, request: UpdateDescriptionRequest): TodoItem {
		val item = getItemOrThrow(id)
		item.assertStillDoable(clock)
		item.description = request.description
		return repository.save(item)
	}

	@Transactional
	fun markDoneById(id: Long): TodoItem {
		val now = Instant.now(clock)
		val item = getItemOrThrow(id)
		item.assertStillDoable(clock)
		item.status = TodoStatus.DONE
		item.doneDatetime = now
		return repository.save(item)
	}

	@Transactional
	fun markNotDoneById(id: Long): TodoItem {
		val item = getItemOrThrow(id)
		item.assertStillDoable(clock)
		item.status = TodoStatus.NOT_DONE
		item.doneDatetime = null
		val saved = repository.save(item)
		return repository.findById(saved.id ?: 0).orElse(saved)
	}

	@Transactional
	fun findById(id: Long): TodoItem {
		val item = getItemOrThrow(id)
		val isTodoNotDoneNowDue = item.isNotDoneNowDue(Instant.now(clock))
		if (isTodoNotDoneNowDue) {
			item.status = TodoStatus.PAST_DUE
			return repository.save(item)
		}
		return item
	}

	@Transactional
	fun findAllOrOnlyDone(includeAll: Boolean): List<TodoItem> {
		updatePastDueTodos()
		return (if (includeAll) {
			repository.findAll()
		} else {
			return repository.findByStatus(TodoStatus.NOT_DONE)
		})
	}

	@Transactional
	@Scheduled(fixedDelayString = "PT1M")
	fun updatePastDueTodos() {
		repository.markPastDue(Instant.now(clock))
	}

	private fun getItemOrThrow(id: Long): TodoItem {
		return repository.findById(id).orElseThrow {
			ResponseStatusException(HttpStatus.NOT_FOUND, "Todo item $id not found")
		}
	}
}