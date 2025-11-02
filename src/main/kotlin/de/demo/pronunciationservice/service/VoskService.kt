package de.demo.pronunciationservice.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.demo.pronunciationservice.model.RecognizedSpeechDto
import de.demo.pronunciationservice.model.WordEvaluationDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayInputStream
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * Service for speech recognition using Vosk ASR.
 * Vosk provides accurate, offline speech recognition with word-level timestamps.
 *
 * @property modelPath Path to the Vosk model directory.
 * @property sampleRate Sample rate for audio processing (default 16000 Hz).
 */
@Service
class VoskService(
    @Value("\${vosk.model-path:}") private val modelPath: String,
    @Value("\${vosk.sample-rate:16000}") private val sampleRate: Float
) {
    private var model: Model? = null
    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(VoskService::class.java)

    @PostConstruct
    fun initialize() {
        // Only initialize if model path is configured
        if (modelPath.isNotBlank()) {
            try {
                model = Model(modelPath)
                logger.info("Vosk model initialized successfully from: {}", modelPath)
            } catch (e: Exception) {
                // Log warning but don't fail - model might be configured later or not needed
                logger.warn("Failed to initialize Vosk model at path: {} - {}", modelPath, e.message)
            }
        } else {
            logger.info("Vosk model path not configured. Vosk transcription will not be available.")
        }
    }

    @PreDestroy
    fun cleanup() {
        model?.close()
    }

    /**
     * Checks if Vosk is properly initialized and ready to use.
     */
    fun isAvailable(): Boolean = model != null

    /**
     * Recognizes speech from the given audio byte array.
     * Audio must be 16-bit mono PCM WAV format at the configured sample rate.
     *
     * @param audioBytes The audio data as a byte array.
     * @return A [RecognizedSpeechDto] containing the recognized transcript and word details.
     * @throws IllegalStateException if Vosk model is not initialized.
     */
    fun recognize(audioBytes: ByteArray): RecognizedSpeechDto {
        val currentModel = model ?: throw IllegalStateException("Vosk model not initialized. Please configure vosk.model-path.")

        val recognizer = Recognizer(currentModel, sampleRate)
        recognizer.setWords(true) // Enable word-level timestamps
        
        try {
            // Skip WAV header (44 bytes) if present
            val audioData = if (audioBytes.size > 44 && isWavHeader(audioBytes)) {
                audioBytes.copyOfRange(44, audioBytes.size)
            } else {
                audioBytes
            }

            val inputStream = ByteArrayInputStream(audioData)
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
                recognizer.acceptWaveForm(buffer, bytesRead)
            }

            // Get final result
            val resultJson = recognizer.finalResult
            val result = objectMapper.readTree(resultJson)

            val transcript = result.get("text")?.asText() ?: ""
            val words = parseWords(result)

            return RecognizedSpeechDto(
                transcript = transcript,
                words = words
            )
        } finally {
            recognizer.close()
        }
    }

    /**
     * Parse word-level results from Vosk JSON response.
     */
    private fun parseWords(result: JsonNode): List<WordEvaluationDto> {
        val resultArray = result.get("result") ?: return emptyList()
        
        return resultArray.mapNotNull { wordNode ->
            val word = wordNode.get("word")?.asText() ?: return@mapNotNull null
            val start = wordNode.get("start")?.asDouble() ?: return@mapNotNull null
            val end = wordNode.get("end")?.asDouble() ?: return@mapNotNull null
            val conf = wordNode.get("conf")?.asDouble()?.toFloat() ?: 1.0f

            WordEvaluationDto(
                word = word,
                startTime = start,
                endTime = end,
                evaluation = conf,
                phonemes = null // Vosk doesn't provide phoneme-level data by default
            )
        }
    }

    /**
     * Check if byte array starts with WAV file header.
     */
    private fun isWavHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        // Check for "RIFF" and "WAVE" markers
        return bytes[0] == 'R'.code.toByte() &&
               bytes[1] == 'I'.code.toByte() &&
               bytes[2] == 'F'.code.toByte() &&
               bytes[3] == 'F'.code.toByte() &&
               bytes[8] == 'W'.code.toByte() &&
               bytes[9] == 'A'.code.toByte() &&
               bytes[10] == 'V'.code.toByte() &&
               bytes[11] == 'E'.code.toByte()
    }
}
