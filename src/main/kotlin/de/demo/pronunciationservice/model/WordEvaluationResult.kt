package de.demo.pronunciationservice.model

/**
 * Represents the evaluation result for a single word.
 *
 * @property word The word text.
 * @property startTime The start time of the word in seconds.
 * @property endTime The end time of the word in seconds.
 * @property evaluation The confidence score for the word.
 * @property phonemes A list of evaluated phonemes for this word.
 */
data class WordEvaluationResult(
    val word: String,
    val startTime: Double,
    val endTime: Double,
    val evaluation: Float,
    val phonemes: List<PhonemeEvaluationResult>? = null
)
