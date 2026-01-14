package com.service.todolist.api

import com.service.todolist.service.PastDueItemException
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(PastDueItemException::class)
	fun handlePastDue(ex: PastDueItemException): ResponseEntity<Map<String, String>> {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(mapOf("error" to (ex.message ?: "Todo item is past due")))
	}

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
		val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(mapOf("error" to message))
	}

	@ExceptionHandler(EntityNotFoundException::class)
	fun handleNotFound(ex: EntityNotFoundException): ResponseEntity<Map<String, String>> {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(mapOf("error" to (ex.message ?: "Todo item not found")))
	}


	@ExceptionHandler(Exception::class)
	fun handleUnexpected(ex: Exception): ResponseEntity<Map<String, String>> {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(mapOf("error" to (ex.message ?: "Unexpected server error")))
	}
}