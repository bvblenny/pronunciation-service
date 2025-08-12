package de.demo.pronunciationservice.model

/**
 * DTO for recognized speech result with word and phoneme details.
 * @property transcript The recognized transcript (hypothesis).
 * @property words List of evaluated words with phoneme details.
 */
data class RecognizedSpeechResult(
    val transcript: String,
    val words: List<WordEvaluationResult>
)

