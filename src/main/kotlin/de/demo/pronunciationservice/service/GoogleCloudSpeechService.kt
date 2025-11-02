package de.demo.pronunciationservice.service

import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import de.demo.pronunciationservice.model.RecognizedSpeechDto
import de.demo.pronunciationservice.model.WordEvaluationDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Service for speech recognition using Google Cloud Speech-to-Text API.
 * Provides cloud-based, highly accurate speech recognition with word-level timestamps.
 *
 * Requires Google Cloud credentials to be configured via:
 * - GOOGLE_APPLICATION_CREDENTIALS environment variable, or
 * - spring.cloud.gcp.credentials.location property
 *
 * @property languageCode Default language code for transcription.
 * @property enableWordTimeOffsets Enable word-level timing information.
 */
@Service
class GoogleCloudSpeechService(
    @Value("\${google.speech.language-code:en-US}") private val defaultLanguageCode: String,
    @Value("\${google.speech.enable-word-time-offsets:true}") private val enableWordTimeOffsets: Boolean
) {
    private var speechClient: SpeechClient? = null
    private val logger = LoggerFactory.getLogger(GoogleCloudSpeechService::class.java)

    companion object {
        private const val SAMPLE_RATE_HZ = 16000
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }

    @PostConstruct
    fun initialize() {
        try {
            speechClient = SpeechClient.create()
            logger.info("Google Cloud Speech-to-Text client initialized successfully")
        } catch (e: Exception) {
            // Log warning but don't fail - credentials might not be configured
            logger.warn("Failed to initialize Google Cloud Speech client: {}. Cloud transcription will not be available.", e.message)
        }
    }

    @PreDestroy
    fun cleanup() {
        speechClient?.close()
    }

    /**
     * Checks if Google Cloud Speech client is properly initialized and ready to use.
     */
    fun isAvailable(): Boolean = speechClient != null

    /**
     * Recognizes speech from the given audio byte array using Google Cloud Speech-to-Text.
     * Audio must be 16-bit mono PCM WAV format at 16kHz.
     *
     * @param audioBytes The audio data as a byte array.
     * @param languageCode The language code (e.g., "en-US", "de-DE"). Defaults to configured value.
     * @return A [RecognizedSpeechDto] containing the recognized transcript and word details.
     * @throws IllegalStateException if Google Cloud Speech client is not initialized.
     */
    fun recognize(audioBytes: ByteArray, languageCode: String = defaultLanguageCode): RecognizedSpeechDto {
        val client = speechClient 
            ?: throw IllegalStateException("Google Cloud Speech client not initialized. Please configure Google Cloud credentials.")

        val audio = RecognitionAudio.newBuilder()
            .setContent(ByteString.copyFrom(audioBytes))
            .build()

        val config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setLanguageCode(languageCode)
            .setSampleRateHertz(SAMPLE_RATE_HZ)
            .setEnableWordTimeOffsets(enableWordTimeOffsets)
            .setEnableWordConfidence(true)
            .setEnableAutomaticPunctuation(false)
            .build()

        return try {
            val response = client.recognize(config, audio)

            if (response.resultsCount == 0) {
                logger.debug("No speech recognition results returned")
                return RecognizedSpeechDto(transcript = "", words = emptyList())
            }

            val result = response.getResults(0)
            if (result.alternativesCount == 0) {
                logger.debug("No alternatives in speech recognition results")
                return RecognizedSpeechDto(transcript = "", words = emptyList())
            }

            val alternative = result.getAlternatives(0)
            val transcript = alternative.transcript
            val words = parseWords(alternative)

            RecognizedSpeechDto(
                transcript = transcript,
                words = words
            )
        } catch (e: Exception) {
            logger.error("Error during Google Cloud speech recognition", e)
            throw IllegalStateException("Failed to recognize speech: ${e.message}", e)
        }
    }

    /**
     * Parse word-level results from Google Cloud Speech API response.
     * Extracts word text, timing, and confidence scores.
     */
    private fun parseWords(alternative: SpeechRecognitionAlternative): List<WordEvaluationDto> {
        return alternative.wordsList.mapIndexed { index, wordInfo ->
            // Convert Google Cloud time offsets (Duration with seconds and nanos) to seconds
            // startTime and endTime fields are available when enableWordTimeOffsets is true
            val startTime = if (wordInfo.hasStartTime()) {
                wordInfo.startTime.seconds + (wordInfo.startTime.nanos / NANOS_PER_SECOND)
            } else {
                0.0
            }
            val endTime = if (wordInfo.hasEndTime()) {
                wordInfo.endTime.seconds + (wordInfo.endTime.nanos / NANOS_PER_SECOND)
            } else {
                0.0
            }
            
            WordEvaluationDto(
                word = wordInfo.word,
                startTime = startTime,
                endTime = endTime,
                evaluation = wordInfo.confidence,
                phonemes = null // Google Cloud API doesn't provide phoneme-level data in basic response
            )
        }
    }
}
