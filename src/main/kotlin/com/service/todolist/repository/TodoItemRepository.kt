package com.service.todolist.repository

import com.service.todolist.model.TodoItem
import com.service.todolist.model.TodoStatus
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface TodoItemRepository : JpaRepository<TodoItem, Long> {
	fun findByStatus(status: TodoStatus, sort: Sort = Sort.by("creationDatetime").ascending()): List<TodoItem>

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(
		"""
        update TodoItem t
        set 
		t.status = com.service.todolist.model.TodoStatus.PAST_DUE,
		t.doneDatetime = :now
        where t.status = com.service.todolist.model.TodoStatus.NOT_DONE
		and t.dueDatetime < :now
        """,
	)
	fun markPastDue(@Param("now") now: Instant): Int
}