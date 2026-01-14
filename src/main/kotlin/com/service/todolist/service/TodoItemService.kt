package com.service.todolist.service

import com.service.todolist.api.CreateTodoItemRequest
import com.service.todolist.api.UpdateDescriptionRequest
import com.service.todolist.model.TodoItem
import com.service.todolist.model.TodoStatus
import com.service.todolist.model.assertStillDoable
import com.service.todolist.model.isNotDoneNowDue
import com.service.todolist.repository.TodoItemRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class TodoItemService(
	private val repository: TodoItemRepository,
	private val clock: Clock,
) {

	private val logger = LoggerFactory.getLogger(TodoItemService::class.java)

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
		val item = findItemOrThrow(id)
		return runCatching {
			item.assertStillDoable(clock)
			item.description = request.description
			repository.save(item)
		}.onFailure {
			logger.error("Cannot update description of past due todo item; id=$id, description='${request.description}', errorMessage='${it.message}'")
			throw it
		}.getOrThrow()
	}

	@Transactional
	fun markDoneById(id: Long): TodoItem {
		val now = Instant.now(clock)
		val item = findItemOrThrow(id)
		return runCatching {
			item.assertStillDoable(clock)
			item.status = TodoStatus.DONE
			item.doneDatetime = now
			repository.save(item)
		}.onFailure {
			logger.error("Cannot mark as done; id=$id, errorMessage='${it.message}'")
			throw it
		}.getOrThrow()
	}

	@Transactional
	fun markNotDoneById(id: Long): TodoItem {
		val item = findItemOrThrow(id)
		return runCatching {
			item.assertStillDoable(clock)
			item.status = TodoStatus.NOT_DONE
			item.doneDatetime = null
			val saved = repository.save(item)
			repository.findById(saved.id ?: 0).orElse(saved)
		}.onFailure {
			logger.error("Cannot mark as not done; id=$id, errorMessage='${it.message}'")
			throw it
		}.getOrThrow()
	}

	@Transactional
	fun findById(id: Long): TodoItem {
		val item = findItemOrThrow(id)
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
		val updatedRows = repository.markPastDue(Instant.now(clock))
		logger.info("Updating not done todo that are now past due; updatedRows=$updatedRows")
	}

	private fun findItemOrThrow(id: Long): TodoItem {
		return repository.findById(id).orElseThrow {
			logger.error("Todo item not found; id=$id")
			EntityNotFoundException("Todo item $id not found")
		}
	}
}