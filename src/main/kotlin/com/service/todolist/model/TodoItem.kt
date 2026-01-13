package com.service.todolist.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "todo_items")
class TodoItem(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	@Column(nullable = false)
	var description: String,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: TodoStatus,

	@Column(nullable = false)
	var creationDatetime: Instant,

	@Column(nullable = false)
	var dueDatetime: Instant,

	@Column
	var doneDatetime: Instant? = null,
)