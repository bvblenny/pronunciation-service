package de.demo.pronunciationservice.model

/**
 * DTO representing the evaluation of a sentence.
 *
 * @property transcript The recognized or provided transcript for the sentence being evaluated.
 * @property words A list of word evaluation details, each represented by WordEvaluationDto.
 */
data class SentenceEvaluationDto(
    val transcript: String,
    val words: List<WordEvaluationDto>
)
