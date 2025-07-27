package de.demo.pronunciationservice.model

/**
 * Represents the complete evaluation result for a sentence or transcript.
 *
 * @property transcript The full text that was analyzed.
 * @property words A list of evaluated words within the transcript.
 */
data class SentenceEvaluationResult(
    val transcript: String,
    val words: List<WordEvaluationResult>
)
