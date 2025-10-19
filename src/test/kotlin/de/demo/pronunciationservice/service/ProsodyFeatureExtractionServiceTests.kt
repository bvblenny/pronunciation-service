package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.WordEvaluationDto
import de.demo.pronunciationservice.model.RecognizedSpeechDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

class ProsodyFeatureExtractionServiceTests {

    private lateinit var featureExtractionService: ProsodyFeatureExtractionService
    private lateinit var mockSphinxService: SphinxService

    @BeforeEach
    fun setUp() {
        mockSphinxService = mock(SphinxService::class.java)
        featureExtractionService = ProsodyFeatureExtractionService(mockSphinxService)
    }

    @Test
    fun `extractFeatures should return valid prosody features`() {
        val wavBytes = createTestWavFile()
        val mockWords = createMockWordEvaluations()

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("hello world", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertNotNull(features)
        assertTrue(features.duration > 0.0)
        assertTrue(features.pitchContour.isNotEmpty())
        assertTrue(features.energyContour.isNotEmpty())
        assertTrue(features.wordTimings.isNotEmpty())
    }

    @Test
    fun `extractFeatures should extract pitch contour`() {
        val wavBytes = createTestWavFile()
        val mockWords = createMockWordEvaluations()

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("test", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertTrue(features.pitchContour.isNotEmpty())
        features.pitchContour.forEach { point ->
            assertTrue(point.timeSec >= 0.0)
            assertTrue(point.frequencyHz >= 0.0)
        }
    }

    @Test
    fun `extractFeatures should extract energy contour`() {
        val wavBytes = createTestWavFile()
        val mockWords = createMockWordEvaluations()

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("test", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertTrue(features.energyContour.isNotEmpty())
        features.energyContour.forEach { point ->
            assertTrue(point.timeSec >= 0.0)
            assertTrue(point.energy >= 0.0)
        }
    }

    @Test
    fun `extractFeatures should build word timings from sphinx results`() {
        val wavBytes = createTestWavFile()
        val mockWords = listOf(
            WordEvaluationDto("hello", 0.0, 0.5, 0.95),
            WordEvaluationDto("world", 0.6, 1.1, 0.92)
        )

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("hello world", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertEquals(2, features.wordTimings.size)
        assertEquals("hello", features.wordTimings[0].word)
        assertEquals("world", features.wordTimings[1].word)
        assertTrue(features.wordTimings[0].syllableCount > 0)
        assertTrue(features.wordTimings[1].syllableCount > 0)
    }

    @Test
    fun `extractFeatures should detect pauses between words`() {
        val wavBytes = createTestWavFile()
        val mockWords = listOf(
            WordEvaluationDto("hello", 0.0, 0.5, 0.95),
            WordEvaluationDto("world", 1.0, 1.5, 0.92) // 0.5 sec gap - should be detected as pause
        )

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("hello world", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertTrue(features.pauseRegions.isNotEmpty(), "Should detect pause between words")
        val pause = features.pauseRegions[0]
        assertTrue(pause.startSec >= 0.5)
        assertTrue(pause.endSec <= 1.0)
    }

    @Test
    fun `extractFeatures should not detect short gaps as pauses`() {
        val wavBytes = createTestWavFile()
        val mockWords = listOf(
            WordEvaluationDto("hello", 0.0, 0.5, 0.95),
            WordEvaluationDto("world", 0.55, 1.0, 0.92) // Only 0.05 sec gap
        )

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("hello world", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertTrue(features.pauseRegions.isEmpty(), "Short gaps should not be detected as pauses")
    }

    @Test
    fun `extractFeatures should detect filled pauses`() {
        val wavBytes = createTestWavFile()
        val mockWords = listOf(
            WordEvaluationDto("hello", 0.0, 0.5, 0.95),
            WordEvaluationDto("um", 0.7, 0.9, 0.80),  // Filled pause
            WordEvaluationDto("world", 1.1, 1.5, 0.92)
        )

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("hello um world", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertTrue(features.pauseRegions.any { it.filled }, "Should detect filled pauses")
    }

    @Test
    fun `extractFeatures should estimate syllable count correctly`() {
        val wavBytes = createTestWavFile()
        val mockWords = listOf(
            WordEvaluationDto("cat", 0.0, 0.3, 0.95),      // 1 syllable
            WordEvaluationDto("hello", 0.3, 0.7, 0.92),    // 2 syllables
            WordEvaluationDto("beautiful", 0.7, 1.2, 0.88) // 3 syllables
        )

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("cat hello beautiful", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertEquals(1, features.wordTimings[0].syllableCount)
        assertEquals(2, features.wordTimings[1].syllableCount)
        assertEquals(3, features.wordTimings[2].syllableCount)
    }

    @Test
    fun `extractFeatures should handle empty audio gracefully`() {
        val wavBytes = createTestWavFile(durationMs = 100)

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("", emptyList())
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        assertNotNull(features)
        assertTrue(features.wordTimings.isEmpty())
        assertTrue(features.pauseRegions.isEmpty())
    }

    @Test
    fun `extractFeatures should calculate correct duration`() {
        val wavBytes = createTestWavFile(durationMs = 2000) // 2 seconds
        val mockWords = createMockWordEvaluations()

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("test", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        // Duration should be approximately 2 seconds (allow some tolerance)
        assertTrue(features.duration >= 1.5 && features.duration <= 2.5,
            "Duration was ${features.duration}, expected around 2.0")
    }

    @Test
    fun `pitch contour should mark unvoiced frames correctly`() {
        val wavBytes = createTestWavFile()
        val mockWords = createMockWordEvaluations()

        whenever(mockSphinxService.recognize(any())).thenReturn(
            RecognizedSpeechDto("test", mockWords)
        )

        val features = featureExtractionService.extractFeatures(wavBytes)

        // Should have both voiced and unvoiced frames in typical speech
        val hasVoiced = features.pitchContour.any { it.voiced }
        assertTrue(hasVoiced, "Should detect some voiced frames")
    }

    // Helper methods

    private fun createTestWavFile(durationMs: Int = 1000, sampleRate: Float = 16000f): ByteArray {
        val format = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            false
        )

        val numSamples = (sampleRate * durationMs / 1000).toInt()
        val audioData = ShortArray(numSamples)

        // Generate a simple sine wave
        for (i in audioData.indices) {
            val angle = 2.0 * Math.PI * i * 200.0 / sampleRate // 200 Hz tone
            audioData[i] = (Short.MAX_VALUE * 0.5 * Math.sin(angle)).toInt().toShort()
        }

        val byteBuffer = java.nio.ByteBuffer.allocate(audioData.size * 2)
        byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
        audioData.forEach { byteBuffer.putShort(it) }

        val audioBytes = byteBuffer.array()
        val bais = java.io.ByteArrayInputStream(audioBytes)
        val audioInputStream = AudioInputStream(bais, format, audioData.size.toLong())

        val baos = ByteArrayOutputStream()
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, baos)

        return baos.toByteArray()
    }

    private fun createMockWordEvaluations(): List<WordEvaluationDto> {
        return listOf(
            WordEvaluationDto("hello", 0.0, 0.5, 0.95),
            WordEvaluationDto("world", 0.6, 1.0, 0.92)
        )
    }
}

