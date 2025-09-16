package de.demo.pronunciationservice.model

/**
 * Detailed analysis of a pronunciation attempt combining recognition, forced alignment and
 * edit-distance based error labels.
 */
 data class DetailedAnalysisDto(
     val referenceText: String,
     val transcript: String,
     val wer: Double,
     val substitutions: Int,
     val insertions: Int,
     val deletions: Int,
     val totalDurationSec: Double?,
     val speechRateWpm: Double?,
     val averageWordDurationSec: Double?,
     val pauses: List<PauseDto>,
     val words: List<WordAnalysisDto>
 )

 data class WordAnalysisDto(
     val index: Int,
     val expected: String?,
     val actual: String?,
     val errorType: ErrorType,
     val startTimeSec: Double?,
     val endTimeSec: Double?,
     val durationSec: Double?,
     val evaluation: Double?,
     val phonemes: List<PhonemeEvaluationDto>? = null
 )

 data class PauseDto(
     val startTimeSec: Double,
     val endTimeSec: Double,
     val durationSec: Double,
     val precedingWord: String?,
     val followingWord: String?
 )

 enum class ErrorType { MATCH, SUBSTITUTION, INSERTION, DELETION }

