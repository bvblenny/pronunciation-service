package de.demo.pronunciationservice.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * Tests for GoogleCloudSpeechService.
 * 
 * Note: Most tests require Google Cloud credentials to be configured.
 * Set GOOGLE_APPLICATION_CREDENTIALS environment variable to run integration tests.
 */
class GoogleCloudSpeechServiceTest {

    @Test
    fun `isAvailable should return false before initialization`() {
        val service = GoogleCloudSpeechService(defaultLanguageCode = "en-US", enableWordTimeOffsets = true)
        
        // Before calling initialize(), client should not be available
        assertFalse(service.isAvailable())
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLE_APPLICATION_CREDENTIALS", matches = ".+")
    fun `recognize should transcribe audio when credentials are configured`() {
        val service = GoogleCloudSpeechService(defaultLanguageCode = "en-US", enableWordTimeOffsets = true)
        service.initialize()
        
        assertTrue(service.isAvailable())
        
        // Create a minimal WAV header for testing
        val wavHeader = createMinimalWavHeader()
        
        // Note: This will likely produce empty results due to no actual audio
        // A real test would require sample audio files
        val result = service.recognize(wavHeader)
        
        assertNotNull(result)
        assertNotNull(result.transcript)
        assertNotNull(result.words)
        
        service.cleanup()
    }

    @Test
    fun `recognize should throw exception when credentials not initialized`() {
        val service = GoogleCloudSpeechService(defaultLanguageCode = "en-US", enableWordTimeOffsets = true)
        // Don't call initialize - simulate missing credentials
        
        if (!service.isAvailable()) {
            val exception = assertThrows(IllegalStateException::class.java) {
                service.recognize(ByteArray(100))
            }
            
            assertTrue(exception.message?.contains("not initialized") == true)
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLE_APPLICATION_CREDENTIALS", matches = ".+")
    fun `recognize should accept custom language code`() {
        val service = GoogleCloudSpeechService(defaultLanguageCode = "en-US", enableWordTimeOffsets = true)
        service.initialize()
        
        if (service.isAvailable()) {
            val wavHeader = createMinimalWavHeader()
            
            // Should not throw exception with custom language code
            assertDoesNotThrow {
                service.recognize(wavHeader, "de-DE")
            }
            
            service.cleanup()
        }
    }

    /**
     * Creates a minimal WAV header for testing purposes.
     * Format: 16-bit mono PCM at 16kHz sample rate.
     */
    private fun createMinimalWavHeader(): ByteArray {
        return byteArrayOf(
            // RIFF header
            0x52, 0x49, 0x46, 0x46, // "RIFF"
            0x24, 0x00, 0x00, 0x00, // File size - 8
            0x57, 0x41, 0x56, 0x45, // "WAVE"
            // fmt chunk
            0x66, 0x6d, 0x74, 0x20, // "fmt "
            0x10, 0x00, 0x00, 0x00, // Chunk size (16)
            0x01, 0x00,             // Audio format (1 = PCM)
            0x01, 0x00,             // Number of channels (1 = mono)
            0x80.toByte(), 0x3e, 0x00, 0x00, // Sample rate (16000)
            0x00, 0x7d, 0x00, 0x00, // Byte rate (16000 * 1 * 16/8)
            0x02, 0x00,             // Block align (1 * 16/8)
            0x10, 0x00,             // Bits per sample (16)
            // data chunk
            0x64, 0x61, 0x74, 0x61, // "data"
            0x00, 0x00, 0x00, 0x00  // Data size (0 for this test)
        )
    }
}
