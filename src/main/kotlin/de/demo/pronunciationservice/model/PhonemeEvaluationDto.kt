package de.demo.pronunciationservice.model

/**
 * Represents the evaluation result for a single phoneme.
 *
 * @property phoneme The phoneme representation (e.g., "AH", "B").
 * @property startTime The start time of the phoneme in seconds.
 * @property endTime The end time of the phoneme in seconds.
 * @property evaluation The confidence score for the phoneme.
 */
data class PhonemeEvaluationDto(
    val phoneme: String,
    val evaluation: Double,
    val startTime: Double?,
    val endTime: Double?,
)
