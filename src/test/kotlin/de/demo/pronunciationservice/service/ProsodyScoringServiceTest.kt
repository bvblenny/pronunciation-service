package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class ProsodyScoringServiceTest {

    private lateinit var scoringService: ProsodyScoringService

    @BeforeEach
    fun setUp() {
        scoringService = ProsodyScoringService()
    }

    @Test
    fun `score should return valid prosody score with all components`() {
        val features = createMockFeatures()

        val result = scoringService.score(features, "Hello world", "en-US")

        assertNotNull(result)
        assertTrue(result.overallScore in 0.0..1.0)
        assertTrue(result.subScores.rhythm in 0.0..1.0)
        assertTrue(result.subScores.intonation in 0.0..1.0)
        assertTrue(result.subScores.stress in 0.0..1.0)
        assertTrue(result.subScores.pacing in 0.0..1.0)
        assertTrue(result.subScores.fluency in 0.0..1.0)
        assertNotNull(result.diagnostics)
        assertNotNull(result.feedback)
        assertNotNull(result.metadata)
        assertEquals(ModelType.HEURISTIC, result.metadata.modelType)
    }

    @Test
    fun `score should detect monotone intonation`() {
        // Create features with flat pitch
        val flatPitchContour = (0..100).map { i ->
            PitchPoint(i * 0.01, 150.0, true) // Constant pitch
        }

        val features = ProsodyFeatures(
            duration = 1.0,
            pitchContour = flatPitchContour,
            energyContour = createMockEnergyContour(100),
            wordTimings = createMockWordTimings(5),
            pauseRegions = emptyList()
        )

        val result = scoringService.score(features, "Test", "en-US")

        assertTrue(result.subScores.intonation < 0.7,
            "Monotone speech should score low on intonation")
        assertTrue(result.diagnostics.intonationMetrics.pitchRangeHz < 30.0)
        assertTrue(result.feedback.any { it.category == ProsodyCategory.INTONATION })
    }

    @Test
    fun `score should detect good natural rhythm`() {
        val features = createFeaturesWithNaturalRhythm()

        val result = scoringService.score(features, "This is natural speech", "en-US")

        assertTrue(result.subScores.rhythm > 0.7,
            "Natural rhythm should score well")
        assertTrue(result.diagnostics.rhythmMetrics.isochronyIndex in 0.2..0.6)
    }

    @Test
    fun `score should detect too fast pacing`() {
        // 10 words in 2 seconds = 300 WPM (too fast)
        val features = ProsodyFeatures(
            duration = 2.0,
            pitchContour = createMockPitchContour(200),
            energyContour = createMockEnergyContour(200),
            wordTimings = createMockWordTimings(10, duration = 2.0),
            pauseRegions = emptyList()
        )

        val result = scoringService.score(features, "Words repeated many times", "en-US")

        assertTrue(result.subScores.pacing < 0.85,
            "Too fast speech should be penalized")
        assertTrue(result.diagnostics.pacingMetrics.wordsPerMinute > 250.0)
        assertTrue(result.feedback.any {
            it.category == ProsodyCategory.PACING && it.message.contains("fast", ignoreCase = true)
        })
    }

    @Test
    fun `score should detect too slow pacing`() {
        // 3 words in 3 seconds = 60 WPM (too slow)
        val features = ProsodyFeatures(
            duration = 3.0,
            pitchContour = createMockPitchContour(300),
            energyContour = createMockEnergyContour(300),
            wordTimings = createMockWordTimings(3, duration = 3.0),
            pauseRegions = emptyList()
        )

        val result = scoringService.score(features, "Very slow", "en-US")

        assertTrue(result.subScores.pacing < 0.75,
            "Too slow speech should be penalized")
        assertTrue(result.diagnostics.pacingMetrics.wordsPerMinute < 100.0)
    }

    @Test
    fun `score should detect poor fluency with many pauses`() {
        val pauses = (1..10).map { i ->
            PauseRegion(i * 0.5, i * 0.5 + 0.4, false)
        }

        val features = ProsodyFeatures(
            duration = 6.0,
            pitchContour = createMockPitchContour(600),
            energyContour = createMockEnergyContour(600),
            wordTimings = createMockWordTimings(8, duration = 6.0),
            pauseRegions = pauses
        )

        val result = scoringService.score(features, "Choppy speech", "en-US")

        assertTrue(result.subScores.fluency < 0.8,
            "Speech with many pauses should score lower on fluency")
        assertTrue(result.diagnostics.fluencyMetrics.pauseCount >= 10)
        assertTrue(result.diagnostics.fluencyMetrics.disfluencyRate > 20.0)
    }

    @Test
    fun `score should detect filled pauses`() {
        val pauses = listOf(
            PauseRegion(1.0, 1.3, true),  // Filled pause
            PauseRegion(2.5, 2.7, true),  // Filled pause
            PauseRegion(4.0, 4.2, false)  // Silent pause
        )

        val features = ProsodyFeatures(
            duration = 5.0,
            pitchContour = createMockPitchContour(500),
            energyContour = createMockEnergyContour(500),
            wordTimings = createMockWordTimings(6, duration = 5.0),
            pauseRegions = pauses
        )

        val result = scoringService.score(features, "Test speech", "en-US")

        assertEquals(2, result.diagnostics.fluencyMetrics.filledPauseCount)
        assertEquals(3, result.diagnostics.fluencyMetrics.pauseCount)
    }

    @Test
    fun `score should provide positive feedback for excellent prosody`() {
        val features = createExcellentProsodyFeatures()

        val result = scoringService.score(features, "Excellent speech", "en-US")

        assertTrue(result.overallScore > 0.85)
        assertTrue(result.feedback.any {
            it.severity == FeedbackSeverity.INFO && it.message.contains("Excellent", ignoreCase = true)
        })
    }

    @Test
    fun `score should provide critical feedback for poor prosody`() {
        val features = createPoorProsodyFeatures()

        val result = scoringService.score(features, "Poor speech", "en-US")

        assertTrue(result.overallScore < 0.6)
        assertTrue(result.feedback.any { it.severity == FeedbackSeverity.CRITICAL })
    }

    @Test
    fun `score should handle empty speech gracefully`() {
        val features = ProsodyFeatures(
            duration = 0.0,
            pitchContour = emptyList(),
            energyContour = emptyList(),
            wordTimings = emptyList(),
            pauseRegions = emptyList()
        )

        val result = scoringService.score(features, "", "en-US")

        assertNotNull(result)
        assertTrue(result.overallScore <= 0.5)
    }

    @Test
    fun `metadata should include version and timestamp`() {
        val features = createMockFeatures()

        val result = scoringService.score(features, "Test", "en-US")

        assertNotNull(result.metadata.scorerVersion)
        assertTrue(result.metadata.scorerVersion.isNotEmpty())
        assertTrue(result.metadata.processingTimestamp > 0)
        assertEquals("en-US", result.metadata.referenceLanguage)
    }

    @Test
    fun `overall score should be weighted combination of sub-scores`() {
        val features = createMockFeatures()

        val result = scoringService.score(features, "Test", "en-US")

        // Verify overall score is reasonable combination
        val expectedRange = listOf(
            result.subScores.rhythm,
            result.subScores.intonation,
            result.subScores.stress,
            result.subScores.pacing,
            result.subScores.fluency
        )

        val minSubScore = expectedRange.minOrNull() ?: 0.0
        val maxSubScore = expectedRange.maxOrNull() ?: 1.0

        assertTrue(result.overallScore in minSubScore..maxSubScore,
            "Overall score should be within range of sub-scores")
    }

    private fun createMockFeatures(): ProsodyFeatures {
        return ProsodyFeatures(
            duration = 3.0,
            pitchContour = createMockPitchContour(300),
            energyContour = createMockEnergyContour(300),
            wordTimings = createMockWordTimings(7),
            pauseRegions = listOf(PauseRegion(1.5, 1.7, false))
        )
    }

    private fun createMockPitchContour(points: Int): List<PitchPoint> {
        return (0 until points).map { i ->
            val time = i * 0.01
            val pitch = 150.0 + 30.0 * kotlin.math.sin(i * 0.1) // Natural variation
            PitchPoint(time, pitch, true)
        }
    }

    private fun createMockEnergyContour(points: Int): List<EnergyPoint> {
        return (0 until points).map { i ->
            val time = i * 0.01
            val energy = 0.3 + 0.2 * kotlin.math.sin(i * 0.15)
            EnergyPoint(time, energy)
        }
    }

    private fun createMockWordTimings(wordCount: Int, duration: Double = 3.0): List<WordTiming> {
        val wordDuration = duration / wordCount
        return (0 until wordCount).map { i ->
            WordTiming(
                word = "word$i",
                startSec = i * wordDuration,
                endSec = (i + 1) * wordDuration,
                syllableCount = 2,
                stressed = i % 2 == 0
            )
        }
    }

    private fun createFeaturesWithNaturalRhythm(): ProsodyFeatures {
        val wordTimings = listOf(
            WordTiming("this", 0.0, 0.3, 1, false),
            WordTiming("is", 0.3, 0.5, 1, false),
            WordTiming("natural", 0.5, 0.9, 3, true),
            WordTiming("speech", 0.9, 1.3, 1, true)
        )

        return ProsodyFeatures(
            duration = 1.3,
            pitchContour = createMockPitchContour(130),
            energyContour = createMockEnergyContour(130),
            wordTimings = wordTimings,
            pauseRegions = emptyList()
        )
    }

    private fun createExcellentProsodyFeatures(): ProsodyFeatures {
        // Target: Overall score > 0.85
        // - Pacing: 8 words in 3.0s -> 160 WPM (optimal)
        // - Rhythm: syllable timing CV ~0.35 (optimal)
        // - Intonation: pitch range ~60-70 Hz, smooth contour
        // - Fluency: no pauses
        // - Stress: high energy contrast so ~50-75% words get stressed
        val duration = 3.0
        val points = (duration * 100).toInt()

        // Smooth, varied pitch with ~60 Hz range (optimal 30..80)
        val variedPitchContour = (0 until points).map { i ->
            val time = i * 0.01
            val pitch = 160.0 + 25.0 * kotlin.math.sin(i * 0.10) + 5.0 * kotlin.math.cos(i * 0.04)
            PitchPoint(time, pitch, true)
        }

        // Design word timings to achieve syllable-duration CV around 0.3-0.35
        val wordSpecs = listOf(
            Triple(2, 0.24, true),
            Triple(2, 0.24, false),
            Triple(2, 0.30, true),
            Triple(2, 0.36, true),
            Triple(2, 0.44, true),
            Triple(2, 0.50, false),
            Triple(2, 0.60, true),
            Triple(2, 0.32, true)
        )

        var t = 0.0
        val naturalWordTimings = wordSpecs.mapIndexed { idx, (syll, dur, stressed) ->
            val end = (t + dur).coerceAtMost(duration)
            val wt = WordTiming("word$idx", t, end, syll, stressed)
            t += dur
            wt
        }

        // Energy: set mean around ~0.59 so threshold mean*1.3 ~0.77; stressed peaks ~0.90 > threshold; unstressed max ~0.40 < threshold
        val energyContour = (0 until points).map { i ->
            val time = i * 0.01
            val wordIdx = naturalWordTimings.indexOfFirst { time >= it.startSec && time <= it.endSec }
            val stressed = if (wordIdx >= 0) naturalWordTimings[wordIdx].stressed else false
            val energy = if (stressed) {
                0.65 + 0.25 * kotlin.math.sin(time * 6.0) // 0.40 .. 0.90
            } else {
                0.35 + 0.05 * kotlin.math.sin(time * 6.0) // 0.30 .. 0.40
            }
            EnergyPoint(time, energy)
        }

        return ProsodyFeatures(
            duration = duration,
            pitchContour = variedPitchContour,
            energyContour = energyContour,
            wordTimings = naturalWordTimings,
            pauseRegions = emptyList()
        )
    }

    private fun createPoorProsodyFeatures(): ProsodyFeatures {
        // Flat pitch (monotone), many pauses, irregular rhythm, extreme pacing
        val flatPitch = (0..500).map { PitchPoint(it * 0.01, 150.0, true) }
        val manyPauses = (1..20).map { i ->
            PauseRegion(i * 0.35, i * 0.35 + 0.6, i % 2 == 0) // More filled pauses
        }

        return ProsodyFeatures(
            duration = 10.0, // Very long duration
            pitchContour = flatPitch,
            energyContour = createMockEnergyContour(700),
            wordTimings = createMockWordTimings(12, 10.0), // too slow
            pauseRegions = manyPauses
        )
    }
}
