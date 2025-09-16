package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.*
import org.springframework.stereotype.Service
import kotlin.math.max

/**
 * Service for performing detailed analysis of speech recognition results.
 * This service analyzes the alignment between a reference text and the recognized speech,
 * calculates Word Error Rate (WER), and provides detailed insights such as pauses, speech rate,
 * and word-level evaluations.
 *
 * @property sphinxService The [SphinxService] used for speech recognition.
 */
@Service
class DetailedAnalysisService(
    private val sphinxService: SphinxService
) {
    /**
     * Analyzes the detailed alignment and evaluation of the given audio data against the reference text.
     *
     * @param wavBytes The audio data as a byte array.
     * @param referenceText The reference text to compare the audio transcription against.
     * @return A [DetailedAnalysisDto] containing the analysis results, including WER, speech rate, and word-level details.
     */
    fun analyzeDetailed(wavBytes: ByteArray, referenceText: String): DetailedAnalysisDto {
        val recognized = sphinxService.recognize(wavBytes)
        val hypothesisWords = recognized.words
        val hypothesisTokens = hypothesisWords.map { normalizeToken(it.word) }
        val referenceTokens = tokenize(referenceText)

        val operations = align(referenceTokens, hypothesisTokens)

        val summary = summarizeOperations(operations, referenceTokens, hypothesisWords)

        val wer = computeWer(
            referenceLength = referenceTokens.size,
            substitutions = summary.substitutions,
            insertions = summary.insertions,
            deletions = summary.deletions,
            hypothesisEmpty = hypothesisTokens.isEmpty()
        )

        val pauses = computePauses(hypothesisWords)
        val (totalDuration, wordsPerMinute, avgWordDuration) = computeDurations(hypothesisWords)

        return DetailedAnalysisDto(
            referenceText = referenceText,
            transcript = recognized.transcript.trim(),
            wer = wer,
            substitutions = summary.substitutions,
            insertions = summary.insertions,
            deletions = summary.deletions,
            totalDurationSec = totalDuration,
            speechRateWpm = wordsPerMinute,
            averageWordDurationSec = avgWordDuration,
            pauses = pauses,
            words = summary.words
        )
    }

    /**
     * Tokenizes the given text into a list of lowercase words, removing extra spaces.
     *
     * @param text The input text to tokenize.
     * @return A list of normalized tokens.
     */
    private fun tokenize(text: String): List<String> =
        text.trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

    /**
     * Normalizes a token by trimming and converting it to lowercase.
     *
     * @param token The token to normalize.
     * @return The normalized token.
     */
    private fun normalizeToken(token: String): String = token.trim().lowercase()

    /**
     * Represents a single alignment step in the dynamic programming alignment process.
     *
     * @property type The type of alignment step (MATCH, SUBSTITUTION, INSERTION, DELETION).
     * @property refIndex The index of the reference token involved in the step, or null for INSERTION.
     * @property hypothesisIndex The index of the hypothesis token involved in the step, or null for DELETION.
     */
    private data class Step(val type: ErrorType, val refIndex: Int?, val hypothesisIndex: Int?)

    /**
     * Represents a summary of alignment operations, including counts of substitutions, insertions, deletions,
     * and a list of word-level analysis results.
     *
     * @property substitutions The number of substitution operations.
     * @property insertions The number of insertion operations.
     * @property deletions The number of deletion operations.
     * @property words The list of word-level analysis results.
     */
    private data class OperationSummary(
        val substitutions: Int,
        val insertions: Int,
        val deletions: Int,
        val words: List<WordAnalysisDto>
    )

    /**
     * Summarizes the alignment operations into counts of substitutions, insertions, deletions,
     * and generates word-level analysis details.
     *
     * @param operations The list of alignment steps.
     * @param referenceTokens The list of reference tokens.
     * @param hypothesisWords The list of hypothesis words with evaluation details.
     * @return An [OperationSummary] containing the summarized results.
     */
    private fun summarizeOperations(
        operations: List<Step>,
        referenceTokens: List<String>,
        hypothesisWords: List<WordEvaluationDto>
    ): OperationSummary {
        var substitutions = 0
        var insertions = 0
        var deletions = 0
        val wordAnalysis = mutableListOf<WordAnalysisDto>()

        operations.forEachIndexed { idx, op ->
            val referenceWord = op.refIndex?.let(referenceTokens::get)
            val hypothesis = op.hypothesisIndex?.let(hypothesisWords::get)

            when (op.type) {
                ErrorType.MATCH ->
                    wordAnalysis += buildFromHypothesis(idx, referenceWord, hypothesis!!, ErrorType.MATCH)
                ErrorType.SUBSTITUTION -> {
                    substitutions++
                    wordAnalysis += buildFromHypothesis(idx, referenceWord, hypothesis!!, ErrorType.SUBSTITUTION)
                }
                ErrorType.INSERTION -> {
                    insertions++
                    wordAnalysis += buildFromHypothesis(idx, null, hypothesis!!, ErrorType.INSERTION)
                }
                ErrorType.DELETION -> {
                    deletions++
                    wordAnalysis += buildDeletion(idx, referenceWord)
                }
            }
        }

        return OperationSummary(
            substitutions = substitutions,
            insertions = insertions,
            deletions = deletions,
            words = wordAnalysis
        )
    }

    /**
     * Computes the Word Error Rate (WER) based on the alignment results.
     *
     * @param referenceLength The length of the reference text.
     * @param substitutions The number of substitution operations.
     * @param insertions The number of insertion operations.
     * @param deletions The number of deletion operations.
     * @param hypothesisEmpty Whether the hypothesis is empty.
     * @return The computed WER as a Double.
     */
    private fun computeWer(
        referenceLength: Int,
        substitutions: Int,
        insertions: Int,
        deletions: Int,
        hypothesisEmpty: Boolean
    ): Double {
        return if (referenceLength == 0) {
            if (hypothesisEmpty) 0.0 else 1.0
        } else (substitutions + deletions + insertions).toDouble() / referenceLength
    }

    /**
     * Aligns two sequences of tokens (reference and hypothesis) using dynamic programming
     * to compute the minimal edit distance. Returns a list of alignment steps.
     *
     * @param reference The list of reference tokens.
     * @param hypothesis The list of hypothesis tokens.
     * @return A list of [Step] objects representing the alignment operations.
     */
    private fun align(reference: List<String>, hypothesis: List<String>): List<Step> {
        val refSize = reference.size
        val hypSize = hypothesis.size
        val dp = Array(refSize + 1) { IntArray(hypSize + 1) }

        // Initialize borders
        for (i in dp.indices) dp[i][0] = i
        for (j in dp[0].indices) dp[0][j] = j

        // Fill DP table
        for (i in 1..refSize) {
            for (j in 1..hypSize) {
                val matchCost = if (reference[i - 1] == hypothesis[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j - 1] + matchCost, // Substitution/Match
                    dp[i - 1][j] + 1,             // Deletion
                    dp[i][j - 1] + 1              // Insertion
                )
            }
        }

        // Backtrace to build steps
        val steps = mutableListOf<Step>()
        var i = refSize
        var j = hypSize
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + if (reference[i - 1] == hypothesis[j - 1]) 0 else 1 -> {
                    val type = if (reference[i - 1] == hypothesis[j - 1]) ErrorType.MATCH else ErrorType.SUBSTITUTION
                    steps.add(Step(type, i - 1, j - 1))
                    i--; j--
                }
                i > 0 && dp[i][j] == dp[i - 1][j] + 1 -> {
                    steps.add(Step(ErrorType.DELETION, i - 1, null))
                    i--
                }
                else -> {
                    steps.add(Step(ErrorType.INSERTION, null, j - 1))
                    j--
                }
            }
        }
        return steps.reversed()
    }

    private fun computeDurations(words: List<WordEvaluationDto>): Triple<Double?, Double?, Double?> {
        if (words.isEmpty()) return Triple(null, null, null)

        val start = words.first().startTime
        val end = words.last().endTime
        val total = max(0.0, end - start)
        val wordsPerMinute = if (total > 0) (words.size / (total / 60.0)) else null
        val avg = words.asSequence()
            .map { (it.endTime - it.startTime).coerceAtLeast(0.0) }
            .average()
            .let { if (it.isNaN()) null else it }

        return Triple(total, wordsPerMinute, avg)
    }

    private fun computePauses(words: List<WordEvaluationDto>, minPauseSec: Double = 0.2): List<PauseDto> {
        if (words.size < 2) return emptyList()

        return words.zipWithNext()
            .mapNotNull { (current, next) ->
                val gap = next.startTime - current.endTime
                if (gap >= minPauseSec) {
                    PauseDto(
                        startTimeSec = current.endTime,
                        endTimeSec = next.startTime,
                        durationSec = gap,
                        precedingWord = current.word,
                        followingWord = next.word
                    )
                } else null
            }
    }

    private fun buildFromHypothesis(
        index: Int,
        expected: String?,
        hypWord: WordEvaluationDto,
        type: ErrorType
    ): WordAnalysisDto = WordAnalysisDto(
        index = index,
        expected = expected,
        actual = hypWord.word,
        errorType = type,
        startTimeSec = hypWord.startTime,
        endTimeSec = hypWord.endTime,
        durationSec = (hypWord.endTime - hypWord.startTime).coerceAtLeast(0.0),
        evaluation = hypWord.evaluation.toDouble(),
        phonemes = hypWord.phonemes
    )

    private fun buildDeletion(index: Int, expected: String?): WordAnalysisDto = WordAnalysisDto(
        index = index,
        expected = expected,
        actual = null,
        errorType = ErrorType.DELETION,
        startTimeSec = null,
        endTimeSec = null,
        durationSec = null,
        evaluation = null,
        phonemes = null
    )
}
