package de.demo.pronunciationservice.strategy

import de.demo.pronunciationservice.model.RecognizedSpeechDto
import de.demo.pronunciationservice.service.GoogleCloudSpeechService
import org.springframework.stereotype.Component

/**
 * Google Cloud Speech-to-Text transcription strategy.
 * 
 * Provides cloud-based speech recognition with the highest accuracy.
 * Requires Google Cloud credentials to be configured.
 * Supports 125+ languages.
 */
@Component
class GoogleCloudTranscriptionStrategy(
    private val googleCloudSpeechService: GoogleCloudSpeechService
) : TranscriptionStrategy {
    
    override fun getProviderNames(): List<String> = listOf("google", "google-cloud", "gcp")
    
    override fun isAvailable(): Boolean = googleCloudSpeechService.isAvailable()
    
    override fun transcribe(audioBytes: ByteArray, languageCode: String): RecognizedSpeechDto {
        if (!isAvailable()) {
            throw IllegalStateException("Google Cloud Speech provider is not available. Please configure Google Cloud credentials.")
        }
        return googleCloudSpeechService.recognize(audioBytes, languageCode)
    }
}
