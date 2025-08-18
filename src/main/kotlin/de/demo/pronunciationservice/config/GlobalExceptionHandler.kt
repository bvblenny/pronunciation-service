package de.demo.pronunciationservice.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException

@ControllerAdvice
class GlobalExceptionHandler {

    data class ErrorBody(val error: String)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorBody> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorBody(ex.message ?: "Bad request"))

    @ExceptionHandler(value = [MaxUploadSizeExceededException::class, MultipartException::class])
    fun handlePayloadTooLarge(ex: Exception): ResponseEntity<ErrorBody> =
        ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ErrorBody("Payload too large"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorBody> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorBody("Validation failed"))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorBody> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorBody("Processing failure"))
}

