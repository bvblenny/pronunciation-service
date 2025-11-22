package de.demo.pronunciationservice.model

import java.time.Instant

data class ErrorResponse(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)
