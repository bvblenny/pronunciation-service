package de.demo.pronunciationservice.model

/**
 * Represents the evaluation result for a single word.
 */
data class WordEvaluationDto(
    val word: String,
    val startTime: Double,
    val endTime: Double,
    val evaluation: Double,
    val phonemes: List<PhonemeEvaluationDto>? = null
)

