package de.demo.pronunciationservice.model

/**
 * Represents details about a single word in a pronunciation evaluation process.
 *
 * @property word The word that was analyzed.
 * @property confidence The confidence score of the pronunciation, typically expressed as a double value between 0 and 1.
 * @property isCorrect Indicates whether the word was pronounced correctly, based on the evaluation.
 * @property expectedWord The expected word in the evaluation, if applicable, or null if not provided.
 */
data class WordDetail(
    val word: String,
    val confidence: Double,
    val isCorrect: Boolean,
    val expectedWord: String?
)

