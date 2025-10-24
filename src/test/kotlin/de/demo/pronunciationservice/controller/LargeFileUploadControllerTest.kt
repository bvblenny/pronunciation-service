package de.demo.pronunciationservice.controller

import de.demo.pronunciationservice.model.PronunciationScoreDto
import de.demo.pronunciationservice.model.WordDetail
import de.demo.pronunciationservice.service.PronunciationService
import de.demo.pronunciationservice.service.TranscriptionService
import de.demo.pronunciationservice.service.SphinxService
import de.demo.pronunciationservice.service.DetailedAnalysisService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * Unit tests for large file upload functionality using mocked services.
 * 
 * These tests verify the controller layer's handling of large file uploads
 * without requiring the full Spring Boot context or Sphinx dependencies.
 */
@WebMvcTest(PronunciationController::class)
class LargeFileUploadControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var pronunciationService: PronunciationService

    @MockBean
    private lateinit var sphinxService: SphinxService

    @MockBean
    private lateinit var transcriptionService: TranscriptionService

    @MockBean
    private lateinit var detailedAnalysisService: DetailedAnalysisService

    /**
     * Generates a WAV file of specified size in MB.
     */
    private fun generateWavFile(targetSizeMB: Int): ByteArray {
        val sampleRate = 16000f
        val channels = 1
        val sampleSizeInBits = 16
        val signed = true
        val bigEndian = false
        
        val format = AudioFormat(
            sampleRate,
            sampleSizeInBits,
            channels,
            signed,
            bigEndian
        )
        
        val targetBytes = (targetSizeMB * 1024 * 1024) - 44
        val bytesPerSample = sampleSizeInBits / 8
        val numSamples = targetBytes / bytesPerSample
        
        val audioData = ByteArray(numSamples * bytesPerSample)
        
        val audioInputStream = AudioInputStream(
            audioData.inputStream(),
            format,
            numSamples.toLong()
        )
        
        val outputStream = ByteArrayOutputStream()
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream)
        
        return outputStream.toByteArray()
    }

    @Test
    @DisplayName("Should accept small file upload (1MB) and return OK")
    fun testSmallFileUpload() {
        val audioBytes = generateWavFile(1)
        val file = MockMultipartFile(
            "audio",
            "test-audio-1mb.wav",
            "audio/wav",
            audioBytes
        )

        // Mock the service behavior
        `when`(transcriptionService.toWavBytes(any())).thenReturn(audioBytes)
        `when`(pronunciationService.evaluate(any(), anyString(), anyString()))
            .thenReturn(
                PronunciationScoreDto(
                    score = 85.0,
                    transcribedText = "Hello world",
                    wordDetails = listOf(
                        WordDetail(word = "Hello", correct = true, confidence = 0.95, startMs = 0L, endMs = 500L),
                        WordDetail(word = "world", correct = true, confidence = 0.90, startMs = 500L, endMs = 1000L)
                    )
                )
            )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "Hello world")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.score").exists())
        .andExpect(jsonPath("$.transcribedText").value("Hello world"))
    }

    @Test
    @DisplayName("Should accept medium file upload (50MB)")
    fun testMediumFileUpload() {
        val audioBytes = generateWavFile(50)
        val file = MockMultipartFile(
            "audio",
            "test-audio-50mb.wav",
            "audio/wav",
            audioBytes
        )

        `when`(transcriptionService.toWavBytes(any())).thenReturn(audioBytes)
        `when`(pronunciationService.evaluate(any(), anyString(), anyString()))
            .thenReturn(
                PronunciationScoreDto(
                    score = 90.0,
                    transcribedText = "This is a test of medium file upload",
                    wordDetails = emptyList()
                )
            )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "This is a test of medium file upload")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.score").value(90.0))
    }

    @Test
    @DisplayName("Should accept large file upload (200MB)")
    fun testLargeFileUpload() {
        val audioBytes = generateWavFile(200)
        val file = MockMultipartFile(
            "audio",
            "test-audio-200mb.wav",
            "audio/wav",
            audioBytes
        )

        `when`(transcriptionService.toWavBytes(any())).thenReturn(audioBytes)
        `when`(pronunciationService.evaluate(any(), anyString(), anyString()))
            .thenReturn(
                PronunciationScoreDto(
                    score = 88.0,
                    transcribedText = "This is a test of large file upload",
                    wordDetails = emptyList()
                )
            )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "This is a test of large file upload")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.score").exists())
    }

    @Test
    @DisplayName("Should accept file close to limit (400MB)")
    fun testNearLimitFileUpload() {
        val audioBytes = generateWavFile(400)
        val file = MockMultipartFile(
            "audio",
            "test-audio-400mb.wav",
            "audio/wav",
            audioBytes
        )

        `when`(transcriptionService.toWavBytes(any())).thenReturn(audioBytes)
        `when`(pronunciationService.evaluate(any(), anyString(), anyString()))
            .thenReturn(
                PronunciationScoreDto(
                    score = 87.0,
                    transcribedText = "This is a test of near-limit file upload",
                    wordDetails = emptyList()
                )
            )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "This is a test of near-limit file upload")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should reject empty file")
    fun testEmptyFileRejected() {
        val file = MockMultipartFile(
            "audio",
            "empty.wav",
            "audio/wav",
            ByteArray(0)
        )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "Hello world")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should accept files without explicit languageCode parameter")
    fun testDefaultLanguageCode() {
        val audioBytes = generateWavFile(1)
        val file = MockMultipartFile(
            "audio",
            "test-audio.wav",
            "audio/wav",
            audioBytes
        )

        `when`(transcriptionService.toWavBytes(any())).thenReturn(audioBytes)
        `when`(pronunciationService.evaluate(any(), anyString(), anyString()))
            .thenReturn(
                PronunciationScoreDto(
                    score = 85.0,
                    transcribedText = "Hello",
                    wordDetails = emptyList()
                )
            )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "Hello")
                // languageCode not specified, should default to en-US
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should handle multiple small files in sequence")
    fun testMultipleSequentialUploads() {
        for (i in 1..5) {
            val audioBytes = generateWavFile(1)
            val file = MockMultipartFile(
                "audio",
                "test-audio-$i.wav",
                "audio/wav",
                audioBytes
            )

            `when`(transcriptionService.toWavBytes(any())).thenReturn(audioBytes)
            `when`(pronunciationService.evaluate(any(), anyString(), anyString()))
                .thenReturn(
                    PronunciationScoreDto(
                        score = 85.0,
                        transcribedText = "Test $i",
                        wordDetails = emptyList()
                    )
                )

            mockMvc.perform(
                multipart("/api/pronunciation/evaluate-stt")
                    .file(file)
                    .param("referenceText", "Test $i")
                    .param("languageCode", "en-US")
            )
            .andExpect(status().isOk)
        }
    }
}
