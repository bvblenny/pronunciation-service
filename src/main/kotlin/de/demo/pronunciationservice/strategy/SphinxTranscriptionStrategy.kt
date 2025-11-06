package de.demo.pronunciationservice.strategy

import de.demo.pronunciationservice.model.RecognizedSpeechDto
import de.demo.pronunciationservice.service.SphinxService
import org.springframework.stereotype.Component

/**
 * Sphinx ASR transcription strategy.
 * 
 * Provides offline speech recognition using CMU Sphinx.
 * This is the default provider and is always available as it's pre-bundled.
 */
@Component
class SphinxTranscriptionStrategy(
    private val sphinxService: SphinxService
) : TranscriptionStrategy {
    
    override fun getProviderNames(): List<String> = listOf("sphinx")
    
    override fun isAvailable(): Boolean = true // Sphinx is always available (pre-bundled)
    
    override fun transcribe(audioBytes: ByteArray, languageCode: String): RecognizedSpeechDto {
        return sphinxService.recognize(audioBytes)
    }
}
