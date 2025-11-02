package de.demo.pronunciationservice.strategy

import de.demo.pronunciationservice.model.RecognizedSpeechDto

/**
 * Strategy interface for ASR (Automatic Speech Recognition) providers.
 * 
 * Each implementation represents a different ASR provider (e.g., Sphinx, Vosk, Google Cloud)
 * following the Strategy design pattern.
 */
interface TranscriptionStrategy {
    
    /**
     * Returns the name(s) of the provider this strategy handles.
     * Multiple names can be supported as aliases (e.g., "google", "google-cloud", "gcp").
     */
    fun getProviderNames(): List<String>
    
    /**
     * Checks if this strategy is available and properly configured.
     * 
     * @return true if the provider is initialized and ready to use, false otherwise
     */
    fun isAvailable(): Boolean
    
    /**
     * Transcribes audio using this provider's ASR engine.
     * 
     * @param audioBytes The audio data as a byte array (16-bit mono PCM WAV at 16kHz)
     * @param languageCode The language code for transcription (e.g., "en-US", "de-DE")
     * @return RecognizedSpeechDto containing the transcript and word-level details
     * @throws IllegalStateException if the provider is not available or configured
     */
    fun transcribe(audioBytes: ByteArray, languageCode: String): RecognizedSpeechDto
}
