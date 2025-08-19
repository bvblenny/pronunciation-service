package de.demo.pronunciationservice.model

/**
 * Represents a pronunciation evaluation score, including transcribed text and word-level details.
 *
 * @property score Overall pronunciation score, typically expressed as a double value between 0 and 1.
 * @property transcribedText The text recognized from the speech input during evaluation.
 * @property wordDetails A list of detailed word evaluations, where each word is assessed in terms of
 * confidence and correctness, along with optional expected information.
 */
data class PronunciationScoreDto(
    val score: Double,
    val transcribedText: String,
    val wordDetails: List<WordDetail>
)

