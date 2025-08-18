package de.demo.pronunciationservice.service

import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.protobuf.ByteString
import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.StreamSpeechRecognizer
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

@Service
class PronunciationService {

    private val logger = Logger.getLogger(PronunciationService::class.java.name)

    /**
     * Analyzes audio and compares it to a reference text to provide a pronunciation score.
     *
     * @param audioBytes The audio data as a ByteArray
     * @param referenceText The expected text that should be pronounced in the audio
     * @param languageCode The language code (e.g., "en-US", "de-DE")
     * @return A score between 0.0 and 1.0 representing how well the audio matches the reference text
     */
    fun scorePronunciation(audioBytes: ByteArray, referenceText: String, languageCode: String): PronunciationScore {
        SpeechClient.create().use { speechClient ->
            val audio = RecognitionAudio.newBuilder()
                .setContent(ByteString.copyFrom(audioBytes))
                .build()

            val config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setLanguageCode(languageCode)
                .setSampleRateHertz(22050)
                .setEnableWordConfidence(true)
                .setEnableAutomaticPunctuation(false)
                .build()

            val response = speechClient.recognize(config, audio)

            if (response.resultsCount == 0) {
                logger.warning("No speech recognition results returned")
                return PronunciationScore(0.0, "No speech detected", emptyList())
            }

            val result = response.getResults(0)
            if (result.alternativesCount == 0) {
                logger.warning("No alternatives in speech recognition results")
                return PronunciationScore(0.0, "No transcription alternatives", emptyList())
            }

            val alternative = result.getAlternatives(0)
            val transcribedText = alternative.transcript.trim()

            // Calculate score based on text similarity and word confidence
            val similarityScore = calculateTextSimilarity(transcribedText, referenceText)
            val confidenceScore = calculateConfidenceScore(alternative)

            // Combine scores (giving more weight to similarity)
            val finalScore = (similarityScore * 0.7) + (confidenceScore * 0.3)

            // Extract word-level details
            val wordDetails = extractWordDetails(alternative, referenceText)

            return PronunciationScore(
                finalScore,
                transcribedText,
                wordDetails
            )
        }
    }

    /**
     * Calculates text similarity using Levenshtein distance
     */
    private fun calculateTextSimilarity(actual: String, expected: String): Double {
        val actualLower = actual.lowercase()
        val expectedLower = expected.lowercase()

        val distance = levenshteinDistance(actualLower, expectedLower)
        val maxLength = max(actualLower.length, expectedLower.length)

        return if (maxLength == 0) 1.0 else (1.0 - distance.toDouble() / maxLength)
    }

    /**
     * Calculates Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        // Create a matrix of size (m+1) x (n+1)
        val dp = Array(m + 1) { IntArray(n + 1) }

        // Initialize the first row and column
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        // Fill the matrix
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[m][n]
    }

    /**
     * Calculates average confidence score from word confidences
     */
    private fun calculateConfidenceScore(alternative: SpeechRecognitionAlternative): Double {
        if (alternative.wordsCount == 0) return 0.0

        var totalConfidence = 0.0
        alternative.wordsList.forEach { wordInfo ->
            totalConfidence += wordInfo.confidence
        }

        return totalConfidence / alternative.wordsCount
    }

    /**
     * Extracts word-level details including confidence scores
     */
    private fun extractWordDetails(alternative: SpeechRecognitionAlternative, referenceText: String): List<WordDetail> {
        val wordDetails = mutableListOf<WordDetail>()

        // Split reference text into words for comparison
        val referenceWords = referenceText.lowercase().split(Regex("\\s+"))
        val recognizedWords = alternative.wordsList

        // Map recognized words to reference words
        recognizedWords.forEachIndexed { index, wordInfo ->
            val word = wordInfo.word
            val confidence = wordInfo.confidence
            val referenceWord = if (index < referenceWords.size) referenceWords[index] else null
            val isCorrect = referenceWord?.equals(word.lowercase(), ignoreCase = true) ?: false

            wordDetails.add(
                WordDetail(
                    word = word,
                    confidence = confidence,
                    isCorrect = isCorrect,
                    expectedWord = referenceWord
                )
            )
        }

        return wordDetails
    }

    /**
     * Performs phoneme-level scoring using CMU Sphinx.
     * @param audioBytes The audio data as a ByteArray
     * @return List of phoneme results with their start/end times
     */
    fun scorePronunciationWithSphinx(audioBytes: ByteArray): List<PhonemeScore> {
        val config = Configuration().apply {
            acousticModelPath = "resource:/edu/cmu/sphinx/models/en-us/en-us"
            dictionaryPath = "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict"
            languageModelPath = "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin"
        }
        val recognizer = StreamSpeechRecognizer(config)
        val stream = ByteArrayInputStream(audioBytes)
        recognizer.startRecognition(stream)
        val phonemeScores = mutableListOf<PhonemeScore>()
        var result = recognizer.result
        while (result != null) {
            val words = result.words
            for (word in words) {
                // Sphinx does not directly output phonemes, but you can get word timings
                // For true phoneme-level, you need to use a phoneme dictionary or aligner
                phonemeScores.add(
                    PhonemeScore(
                        phoneme = word.word.toString(),
                        startTime = word.timeFrame.start / 1000.0,
                        endTime = word.timeFrame.end / 1000.0,
                        confidence = word.confidence
                    )
                )
            }
            result = recognizer.getResult()
        }
        recognizer.stopRecognition()
        return phonemeScores
    }

    data class PhonemeScore(
        val phoneme: String,
        val startTime: Double,
        val endTime: Double,
        val confidence: Double
    )
}

/**
 * Represents the overall pronunciation score and details
 */
data class PronunciationScore(
    val score: Double,                // Overall score between 0.0 and 1.0
    val transcribedText: String,      // Text as recognized by the Speech-to-Text API
    val wordDetails: List<WordDetail> // Word-level details
)

/**
 * Represents details about a single word's pronunciation
 */
data class WordDetail(
    val word: String,
    val confidence: Float,
    val isCorrect: Boolean,
    val expectedWord: String?
)
