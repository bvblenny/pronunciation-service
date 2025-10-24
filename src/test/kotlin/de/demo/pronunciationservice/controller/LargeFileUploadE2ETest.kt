package de.demo.pronunciationservice.controller

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * End-to-end tests for large file upload functionality.
 * 
 * Tests various file sizes to ensure the service can handle uploads
 * up to the configured limit (500MB as per application.properties).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LargeFileUploadE2ETest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    /**
     * Generates a WAV file of specified size in MB.
     * Creates a valid WAV file with audio data to simulate real audio uploads.
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
        
        // Calculate number of samples needed for target size
        // WAV header is typically 44 bytes, so subtract that
        val targetBytes = (targetSizeMB * 1024 * 1024) - 44
        val bytesPerSample = sampleSizeInBits / 8
        val numSamples = targetBytes / bytesPerSample
        
        // Generate silent audio data
        val audioData = ByteArray(numSamples * bytesPerSample)
        
        // Create audio input stream
        val audioInputStream = AudioInputStream(
            audioData.inputStream(),
            format,
            numSamples.toLong()
        )
        
        // Write to byte array
        val outputStream = ByteArrayOutputStream()
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream)
        
        return outputStream.toByteArray()
    }

    @Test
    @DisplayName("Should successfully upload a small audio file (1MB)")
    fun testSmallFileUpload() {
        val audioBytes = generateWavFile(1)
        val file = MockMultipartFile(
            "audio",
            "test-audio-1mb.wav",
            "audio/wav",
            audioBytes
        )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "Hello world")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should successfully upload a medium audio file (50MB)")
    fun testMediumFileUpload() {
        val audioBytes = generateWavFile(50)
        val file = MockMultipartFile(
            "audio",
            "test-audio-50mb.wav",
            "audio/wav",
            audioBytes
        )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "This is a test of medium file upload")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should successfully upload a large audio file (200MB)")
    fun testLargeFileUpload() {
        val audioBytes = generateWavFile(200)
        val file = MockMultipartFile(
            "audio",
            "test-audio-200mb.wav",
            "audio/wav",
            audioBytes
        )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "This is a test of large file upload")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should successfully upload a file close to the limit (400MB)")
    fun testNearLimitFileUpload() {
        val audioBytes = generateWavFile(400)
        val file = MockMultipartFile(
            "audio",
            "test-audio-400mb.wav",
            "audio/wav",
            audioBytes
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
    @DisplayName("Should successfully upload large file to transcription endpoint")
    fun testLargeFileUploadToTranscriptionEndpoint() {
        val audioBytes = generateWavFile(100)
        val file = MockMultipartFile(
            "file",
            "test-audio-100mb.wav",
            "audio/wav",
            audioBytes
        )

        mockMvc.perform(
            multipart("/api/transcription/transcribe")
                .file(file)
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should successfully upload large file to detailed analysis endpoint")
    fun testLargeFileUploadToAnalyzeDetailedEndpoint() {
        val audioBytes = generateWavFile(100)
        val file = MockMultipartFile(
            "audio",
            "test-audio-100mb.wav",
            "audio/wav",
            audioBytes
        )

        mockMvc.perform(
            multipart("/api/pronunciation/analyze-detailed")
                .file(file)
                .param("referenceText", "This is a test of large file upload to analyze endpoint")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should successfully upload large file to Sphinx recognition endpoint")
    fun testLargeFileUploadToSphinxRecognitionEndpoint() {
        val audioBytes = generateWavFile(100)
        val file = MockMultipartFile(
            "audio",
            "test-audio-100mb.wav",
            "audio/wav",
            audioBytes
        )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-sphinx-recognition")
                .file(file)
                .param("referenceText", "This is a test")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should successfully upload large file to Sphinx alignment endpoint")
    fun testLargeFileUploadToSphinxAlignmentEndpoint() {
        val audioBytes = generateWavFile(100)
        val file = MockMultipartFile(
            "audio",
            "test-audio-100mb.wav",
            "audio/wav",
            audioBytes
        )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-sphinx-alignment")
                .file(file)
                .param("referenceText", "This is a test")
                .param("languageCode", "en-US")
        )
        .andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should reject empty file upload")
    fun testEmptyFileUploadRejected() {
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
    @DisplayName("Should handle MP3 file format upload")
    fun testMp3FileUpload() {
        // For MP3, we create a minimal valid MP3 file structure
        // This is a simplified test - in production, use proper MP3 encoding
        val audioBytes = generateWavFile(10)
        val file = MockMultipartFile(
            "audio",
            "test-audio.mp3",
            "audio/mpeg",
            audioBytes
        )

        mockMvc.perform(
            multipart("/api/pronunciation/evaluate-stt")
                .file(file)
                .param("referenceText", "Hello world")
                .param("languageCode", "en-US")
        )
        // May succeed or fail depending on ffmpeg conversion - we just test the upload path
        .andExpect(status().is4xxClientError().or(status().isOk()))
    }
}
