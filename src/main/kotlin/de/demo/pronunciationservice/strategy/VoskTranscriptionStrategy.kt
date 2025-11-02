package de.demo.pronunciationservice.strategy

import de.demo.pronunciationservice.model.RecognizedSpeechDto
import de.demo.pronunciationservice.service.VoskService
import org.springframework.stereotype.Component

/**
 * Vosk ASR transcription strategy.
 * 
 * Provides offline speech recognition using Vosk with high accuracy.
 * Requires a Vosk model to be downloaded and configured.
 */
@Component
class VoskTranscriptionStrategy(
    private val voskService: VoskService
) : TranscriptionStrategy {
    
    override fun getProviderNames(): List<String> = listOf("vosk")
    
    override fun isAvailable(): Boolean = voskService.isAvailable()
    
    override fun transcribe(audioBytes: ByteArray, languageCode: String): RecognizedSpeechDto {
        if (!isAvailable()) {
            throw IllegalStateException("Vosk provider is not available. Please configure vosk.model-path.")
        }
        return voskService.recognize(audioBytes)
    }
}
