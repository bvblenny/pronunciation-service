package de.demo.pronunciationservice.controller

import de.demo.pronunciationservice.model.*
import de.demo.pronunciationservice.service.ProsodyFeatureExtractionService
import de.demo.pronunciationservice.service.ProsodyScoringService
import de.demo.pronunciationservice.service.TranscriptionService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

@WebMvcTest(ProsodyController::class)
class ProsodyControllerIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var transcriptionService: TranscriptionService

    @MockBean
    private lateinit var featureExtractionService: ProsodyFeatureExtractionService

    @MockBean
    private lateinit var scoringService: ProsodyScoringService

    @Test
    fun `POST evaluate should return prosody score`() {
        val audioFile = createMockAudioFile()
        val mockFeatures = createMockFeatures()
        val mockScore = createMockProsodyScore()

        whenever(transcriptionService.toWavBytes(any())).thenReturn(ByteArray(1000))
        whenever(featureExtractionService.extractFeatures(any())).thenReturn(mockFeatures)
        whenever(scoringService.score(any(), any(), any())).thenReturn(mockScore)

        mockMvc.perform(
            multipart("/api/prosody/evaluate")
                .file(audioFile)
                .param("referenceText", "Hello world")
                .param("languageCode", "en-US")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.overallScore").isNumber)
            .andExpect(jsonPath("$.subScores").exists())
            .andExpect(jsonPath("$.subScores.rhythm").isNumber)
            .andExpect(jsonPath("$.subScores.intonation").isNumber)
            .andExpect(jsonPath("$.subScores.stress").isNumber)
            .andExpect(jsonPath("$.subScores.pacing").isNumber)
            .andExpect(jsonPath("$.subScores.fluency").isNumber)
            .andExpect(jsonPath("$.diagnostics").exists())
            .andExpect(jsonPath("$.feedback").isArray)
            .andExpect(jsonPath("$.features").exists())
            .andExpect(jsonPath("$.metadata").exists())
            .andExpect(jsonPath("$.metadata.scorerVersion").exists())
            .andExpect(jsonPath("$.metadata.modelType").value("HEURISTIC"))
    }

    @Test
    fun `POST evaluate should handle missing audio file`() {
        val emptyFile = MockMultipartFile("audio", "test.wav", "audio/wav", ByteArray(0))

        mockMvc.perform(
            multipart("/api/prosody/evaluate")
                .file(emptyFile)
                .param("referenceText", "Test")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST evaluate should use default language code`() {
        val audioFile = createMockAudioFile()
        val mockFeatures = createMockFeatures()
        val mockScore = createMockProsodyScore()

        whenever(transcriptionService.toWavBytes(any())).thenReturn(ByteArray(1000))
        whenever(featureExtractionService.extractFeatures(any())).thenReturn(mockFeatures)
        whenever(scoringService.score(any(), any(), any())).thenReturn(mockScore)

        mockMvc.perform(
            multipart("/api/prosody/evaluate")
                .file(audioFile)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.metadata.referenceLanguage").value("en-US"))
    }

    @Test
    fun `POST features should return raw features`() {
        val audioFile = createMockAudioFile()
        val mockFeatures = createMockFeatures()

        whenever(transcriptionService.toWavBytes(any())).thenReturn(ByteArray(1000))
        whenever(featureExtractionService.extractFeatures(any())).thenReturn(mockFeatures)

        mockMvc.perform(
            multipart("/api/prosody/features")
                .file(audioFile)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.duration").isNumber)
            .andExpect(jsonPath("$.pitchContour").isArray)
            .andExpect(jsonPath("$.energyContour").isArray)
            .andExpect(jsonPath("$.wordTimings").isArray)
            .andExpect(jsonPath("$.pauseRegions").isArray)
    }

    @Test
    fun `GET health should return service status`() {
        mockMvc.perform(get("/api/prosody/health"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("prosody-evaluation"))
            .andExpect(jsonPath("$.scorerVersion").exists())
            .andExpect(jsonPath("$.capabilities").exists())
    }

    @Test
    fun `POST evaluate should include all feedback categories`() {
        val audioFile = createMockAudioFile()
        val mockFeatures = createMockFeatures()
        val mockScore = createMockProsodyScoreWithAllFeedback()

        whenever(transcriptionService.toWavBytes(any())).thenReturn(ByteArray(1000))
        whenever(featureExtractionService.extractFeatures(any())).thenReturn(mockFeatures)
        whenever(scoringService.score(any(), any(), any())).thenReturn(mockScore)

        mockMvc.perform(
            multipart("/api/prosody/evaluate")
                .file(audioFile)
                .param("referenceText", "Test speech")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.feedback").isArray)
            .andExpect(jsonPath("$.feedback[0].category").exists())
            .andExpect(jsonPath("$.feedback[0].severity").exists())
            .andExpect(jsonPath("$.feedback[0].message").exists())
            .andExpect(jsonPath("$.feedback[0].suggestion").exists())
    }

    // Helper methods

    private fun createMockAudioFile(): MockMultipartFile {
        val wavData = createSimpleWavData()
        return MockMultipartFile("audio", "test.wav", "audio/wav", wavData)
    }

    private fun createSimpleWavData(): ByteArray {
        val format = AudioFormat(16000f, 16, 1, true, false)
        val numSamples = 16000 // 1 second
        val audioData = ShortArray(numSamples) { (it % 100).toShort() }

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

    private fun createMockFeatures(): ProsodyFeatures {
        return ProsodyFeatures(
            duration = 2.0,
            pitchContour = listOf(
                PitchPoint(0.0, 150.0, true),
                PitchPoint(0.5, 180.0, true),
                PitchPoint(1.0, 160.0, true)
            ),
            energyContour = listOf(
                EnergyPoint(0.0, 0.5),
                EnergyPoint(0.5, 0.7),
                EnergyPoint(1.0, 0.4)
            ),
            wordTimings = listOf(
                WordTiming("hello", 0.0, 0.5, 2, true),
                WordTiming("world", 0.6, 1.1, 1, false)
            ),
            pauseRegions = emptyList()
        )
    }

    private fun createMockProsodyScore(): ProsodyScoreDto {
        return ProsodyScoreDto(
            overallScore = 0.85,
            subScores = ProsodySubScores(
                rhythm = 0.88,
                intonation = 0.82,
                stress = 0.85,
                pacing = 0.90,
                fluency = 0.80
            ),
            diagnostics = ProsodyDiagnostics(
                rhythmMetrics = RhythmMetrics(0.12, 0.35, 0.34, "Natural rhythm"),
                intonationMetrics = IntonationMetrics(60.0, 0.12, 165.0, 0.85, "Good variation"),
                stressMetrics = StressMetrics(4, 5, 0.8, 2.5, "Good stress"),
                pacingMetrics = PacingMetrics(4.5, 160.0, 140.0, 180.0, "Appropriate pace"),
                fluencyMetrics = FluencyMetrics(2, 0, 0, 0.25, 8.0, "Good fluency")
            ),
            feedback = listOf(
                ProsodyFeedback(
                    category = ProsodyCategory.OVERALL,
                    severity = FeedbackSeverity.INFO,
                    message = "Excellent prosody",
                    suggestion = "Keep up the good work"
                )
            ),
            features = createMockFeatures(),
            metadata = ProsodyMetadata(
                scorerVersion = "1.0.0-heuristic",
                modelType = ModelType.HEURISTIC,
                referenceLanguage = "en-US",
                processingTimestamp = System.currentTimeMillis()
            )
        )
    }

    private fun createMockProsodyScoreWithAllFeedback(): ProsodyScoreDto {
        val baseScore = createMockProsodyScore()
        return baseScore.copy(
            feedback = listOf(
                ProsodyFeedback(
                    ProsodyCategory.RHYTHM,
                    FeedbackSeverity.WARNING,
                    "Rhythm needs improvement",
                    "Practice with metronome"
                ),
                ProsodyFeedback(
                    ProsodyCategory.INTONATION,
                    FeedbackSeverity.INFO,
                    "Good pitch variation",
                    "Keep it up"
                ),
                ProsodyFeedback(
                    ProsodyCategory.PACING,
                    FeedbackSeverity.CRITICAL,
                    "Too fast",
                    "Slow down"
                )
            )
        )
    }
}

