package com.service.todolist.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class TodoStatus(@get:JsonValue val value: String) {
	NOT_DONE("not done"),
	DONE("done"),
	PAST_DUE("past due");

	companion object {
		@JvmStatic
		@JsonCreator
		fun fromValue(value: String): TodoStatus {
			return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
				?: throw IllegalArgumentException("Unknown status: $value")
		}
	}
}