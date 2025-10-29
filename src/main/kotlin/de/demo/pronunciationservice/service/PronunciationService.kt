package de.demo.pronunciationservice.service

import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.protobuf.ByteString
import de.demo.pronunciationservice.model.PronunciationScoreDto
import de.demo.pronunciationservice.model.WordDetail
import org.springframework.stereotype.Service
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

@Service
class PronunciationService {

    private val logger = Logger.getLogger(PronunciationService::class.java.name)
    
    // Reuse SpeechClient instance instead of creating a new one per request
    private val speechClient: SpeechClient by lazy { SpeechClient.create() }

    /**
     * Analyzes audio and compares it to a reference text to provide a pronunciation score.
     *
     * @param audioBytes The audio data as a ByteArray (expected LINEAR16 16kHz mono)
     * @param referenceText The expected text that should be pronounced in the audio
     * @param languageCode The language code (e.g., "en-US", "de-DE")
     * @return A score between 0.0 and 1.0 representing how well the audio matches the reference text
     */
    fun evaluate(audioBytes: ByteArray, referenceText: String, languageCode: String): PronunciationScoreDto {
        val audio = RecognitionAudio.newBuilder()
            .setContent(ByteString.copyFrom(audioBytes))
            .build()

        val config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setLanguageCode(languageCode)
            .setSampleRateHertz(16000)
            .setEnableWordConfidence(true)
            .setEnableAutomaticPunctuation(false)
            .build()

        val response = speechClient.recognize(config, audio)

        if (response.resultsCount == 0) {
            logger.warning("No speech recognition results returned")
            return PronunciationScoreDto(0.0, "No speech detected", emptyList())
        }

        val result = response.getResults(0)
        if (result.alternativesCount == 0) {
            logger.warning("No alternatives in speech recognition results")
            return PronunciationScoreDto(0.0, "No transcription alternatives", emptyList())
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

        return PronunciationScoreDto(
            finalScore,
            transcribedText,
            wordDetails
        )
    }


    private fun calculateTextSimilarity(actual: String, expected: String): Double {
        val actualLower = actual.lowercase()
        val expectedLower = expected.lowercase()

        val distance = levenshteinDistance(actualLower, expectedLower)
        val maxLength = max(actualLower.length, expectedLower.length)

        return if (maxLength == 0) 1.0 else (1.0 - distance.toDouble() / maxLength)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        
        // Handle edge cases
        if (m == 0) return n
        if (n == 0) return m

        // Use space-optimized algorithm: only keep two rows instead of full matrix
        var prevRow = IntArray(n + 1) { it }
        var currRow = IntArray(n + 1)

        for (i in 1..m) {
            currRow[0] = i
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currRow[j] = min(
                    min(prevRow[j] + 1, currRow[j - 1] + 1),
                    prevRow[j - 1] + cost
                )
            }
            // Swap rows
            val temp = prevRow
            prevRow = currRow
            currRow = temp
        }

        return prevRow[n]
    }

    private fun calculateConfidenceScore(alternative: SpeechRecognitionAlternative): Double {
        if (alternative.wordsCount == 0) return 0.0

        var totalConfidence = 0.0
        alternative.wordsList.forEach { wordInfo ->
            totalConfidence += wordInfo.confidence
        }

        return totalConfidence / alternative.wordsCount
    }

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
}
