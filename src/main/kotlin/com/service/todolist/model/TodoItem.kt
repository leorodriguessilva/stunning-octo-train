package com.service.todolist.model

import com.service.todolist.service.PastDueItemException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.Clock
import java.time.Instant

@Entity
@Table(name = "todo_items")
data class TodoItem(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,

	@Column(nullable = false)
	var description: String,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: TodoStatus,

	@Column(nullable = false)
	val creationDatetime: Instant,

	@Column(nullable = false)
	val dueDatetime: Instant,

	@Column
	var doneDatetime: Instant? = null,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null) return false
		if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
		other as TodoItem
		return id != null && id == other.id
	}

	override fun hashCode(): Int = Hibernate.getClass(this).hashCode()

	override fun toString(): String {
		return "TodoItem(id=$id, description='$description', status=$status, creationDatetime=$creationDatetime, dueDatetime=$dueDatetime, doneDatetime=$doneDatetime)"
	}
}

fun TodoItem.isNotDoneNowDue(now: Instant) = this.status == TodoStatus.NOT_DONE && this.dueDatetime < now

fun TodoItem.assertStillDoable(clock: Clock) {
	val isTodoNotDoneNowDue = isNotDoneNowDue(Instant.now(clock)) || status == TodoStatus.PAST_DUE
	if (isTodoNotDoneNowDue) {
		throw PastDueItemException("Cannot modify past due item with id ${this.id}")
	}
}
