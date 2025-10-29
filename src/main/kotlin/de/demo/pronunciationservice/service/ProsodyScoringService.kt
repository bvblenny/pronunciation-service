package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.*
import org.springframework.stereotype.Service
import kotlin.math.*

/**
 * Service for scoring prosody features with explainability.
 * Designed to be extensible - individual scorers can be replaced with ML models
 * without breaking the API contract.
 */
@Service
class ProsodyScoringService {

    companion object {
        private const val SCORER_VERSION = "1.0.0-heuristic"

        // Optimal ranges for speech metrics (can be calibrated per language/accent)
        private const val OPTIMAL_WPM_MIN = 140.0
        private const val OPTIMAL_WPM_MAX = 180.0
        private const val OPTIMAL_SYLLABLES_PER_SEC_MIN = 3.5
        private const val OPTIMAL_SYLLABLES_PER_SEC_MAX = 5.5
        private const val OPTIMAL_PITCH_RANGE_HZ = 50.0
        private const val LONG_PAUSE_THRESHOLD_SEC = 0.5

        // Scoring weights (can be tuned or learned)
        private const val RHYTHM_WEIGHT = 0.20
        private const val INTONATION_WEIGHT = 0.20
        private const val STRESS_WEIGHT = 0.15
        private const val PACING_WEIGHT = 0.25
        private const val FLUENCY_WEIGHT = 0.20
    }

    /**
     * Score prosody features and generate diagnostic feedback.
     *
     * @param features Extracted prosody features
     * @param referenceText Original reference text for context
     * @param languageCode Language code for language-specific norms
     * @return Complete prosody score with diagnostics and feedback
     */
    fun score(
        features: ProsodyFeatures,
        @Suppress("UNUSED_PARAMETER") referenceText: String,
        languageCode: String
    ): ProsodyScoreDto {

        val rhythmResult = scoreRhythm(features)
        val intonationResult = scoreIntonation(features)
        val stressResult = scoreStress(features)
        val pacingResult = scorePacing(features)
        val fluencyResult = scoreFluency(features)

        val overallScore = calculateOverallScore(
            rhythmResult.first,
            intonationResult.first,
            stressResult.first,
            pacingResult.first,
            fluencyResult.first
        )

        val feedback = generateFeedback(
            rhythmResult,
            intonationResult,
            stressResult,
            pacingResult,
            fluencyResult
        )

        return ProsodyScoreDto(
            overallScore = overallScore,
            subScores = ProsodySubScores(
                rhythm = rhythmResult.first,
                intonation = intonationResult.first,
                stress = stressResult.first,
                pacing = pacingResult.first,
                fluency = fluencyResult.first
            ),
            diagnostics = ProsodyDiagnostics(
                rhythmMetrics = rhythmResult.second,
                intonationMetrics = intonationResult.second,
                stressMetrics = stressResult.second,
                pacingMetrics = pacingResult.second,
                fluencyMetrics = fluencyResult.second
            ),
            feedback = feedback,
            features = features,
            metadata = ProsodyMetadata(
                scorerVersion = SCORER_VERSION,
                modelType = ModelType.HEURISTIC,
                referenceLanguage = languageCode,
                processingTimestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Score rhythm based on syllable timing regularity.
     * Future: Replace with learned model from native speaker corpus.
     * Optimized to calculate mean and variance in a single pass.
     */
    private fun scoreRhythm(features: ProsodyFeatures): Pair<Double, RhythmMetrics> {
        val wordTimings = features.wordTimings

        if (wordTimings.isEmpty()) {
            return Pair(0.0, RhythmMetrics(0.0, 0.0, 0.0, "No speech detected"))
        }

        // Calculate syllable durations
        val syllableDurations = wordTimings.mapNotNull { word ->
            val duration = word.endSec - word.startSec
            if (word.syllableCount > 0) duration / word.syllableCount else null
        }

        if (syllableDurations.isEmpty()) {
            return Pair(0.5, RhythmMetrics(0.0, 0.0, 0.5, "Insufficient data for rhythm analysis"))
        }

        // Calculate mean and variance in a single pass for efficiency
        var sum = 0.0
        var sumSquares = 0.0
        for (duration in syllableDurations) {
            sum += duration
            sumSquares += duration * duration
        }
        val count = syllableDurations.size
        val mean = sum / count
        // Use max to handle potential floating-point precision errors
        val variance = maxOf(0.0, (sumSquares / count) - (mean * mean))
        val stdDev = sqrt(variance)
        val cv = if (mean > 0) stdDev / mean else 0.0

        // Expected variance for natural speech (calibrated value)
        val expectedCv = 0.35

        // Isochrony index (lower = more regular, 0.5-0.7 is natural)
        val isochronyIndex = cv

        // Score: penalize both too regular (robotic) and too irregular
        val score = when {
            cv < 0.2 -> 0.6 // Too regular (robotic)
            cv in 0.2..0.5 -> 1.0 - abs(cv - expectedCv) / expectedCv
            else -> max(0.3, 1.0 - (cv - 0.5) / 0.5)
        }

        val interpretation = when {
            cv < 0.2 -> "Speech rhythm is too regular, sounds robotic"
            cv in 0.2..0.5 -> "Natural rhythm with appropriate variation"
            else -> "Irregular rhythm, consider more consistent pacing"
        }

        return Pair(
            score,
            RhythmMetrics(
                syllableTimingVariance = variance,
                expectedVariance = expectedCv,
                isochronyIndex = isochronyIndex,
                interpretation = interpretation
            )
        )
    }

    /**
     * Score intonation based on pitch variation and contour.
     * Future: Replace with prosodic pattern matching against target language.
     * Optimized to calculate statistics in a single pass.
     */
    private fun scoreIntonation(features: ProsodyFeatures): Pair<Double, IntonationMetrics> {
        val voicedPitches = features.pitchContour.filter { it.voiced && it.frequencyHz > 0 }

        if (voicedPitches.isEmpty()) {
            return Pair(0.0, IntonationMetrics(0.0, 0.0, 0.0, 0.0, "No voiced speech detected"))
        }

        // Calculate all statistics in a single pass
        var sum = 0.0
        var sumSquares = 0.0
        var minPitch = Double.MAX_VALUE
        var maxPitch = Double.MIN_VALUE
        
        for (pitch in voicedPitches) {
            val freq = pitch.frequencyHz
            sum += freq
            sumSquares += freq * freq
            if (freq < minPitch) minPitch = freq
            if (freq > maxPitch) maxPitch = freq
        }
        
        val count = voicedPitches.size
        val meanPitch = sum / count
        // Use max to handle potential floating-point precision errors
        val variance = maxOf(0.0, (sumSquares / count) - (meanPitch * meanPitch))
        val stdDev = sqrt(variance)
        val cv = if (meanPitch > 0) stdDev / meanPitch else 0.0
        val pitchRange = maxPitch - minPitch

        // Calculate contour smoothness (less jagged = more natural)
        val smoothness = calculateContourSmoothness(voicedPitches)

        // Score based on pitch range and variation
        val rangeScore = when {
            pitchRange < 20.0 -> 0.3 // Monotone
            pitchRange in 20.0..30.0 -> 0.6
            pitchRange in 30.0..80.0 -> 1.0
            pitchRange in 80.0..120.0 -> 0.9
            else -> 0.7 // Overly dramatic
        }

        val variationScore = when {
            cv < 0.05 -> 0.3 // Too flat
            cv in 0.05..0.15 -> 1.0
            else -> 0.7 // Too variable
        }

        val score = (rangeScore * 0.6 + variationScore * 0.2 + smoothness * 0.2)

        val interpretation = when {
            pitchRange < 30.0 -> "Monotone intonation, add more pitch variation"
            pitchRange in 30.0..80.0 -> "Good pitch variation and natural intonation"
            else -> "Very wide pitch range, may sound exaggerated"
        }

        return Pair(
            score,
            IntonationMetrics(
                pitchRangeHz = pitchRange,
                pitchVariationCoefficient = cv,
                meanPitchHz = meanPitch,
                contourSmoothness = smoothness,
                interpretation = interpretation
            )
        )
    }

    /**
     * Calculate smoothness of pitch contour
     */
    private fun calculateContourSmoothness(pitches: List<PitchPoint>): Double {
        if (pitches.size < 2) return 1.0

        val deltas = pitches.zipWithNext { a, b -> abs(b.frequencyHz - a.frequencyHz) }
        val meanDelta = deltas.average()

        // Smoother contours have smaller average pitch changes
        return max(0.0, 1.0 - meanDelta / 50.0)
    }

    /**
     * Score stress patterns based on energy contrast.
     * Future: Replace with phoneme-level stress detection.
     */
    private fun scoreStress(features: ProsodyFeatures): Pair<Double, StressMetrics> {
        val wordTimings = features.wordTimings
        val energyContour = features.energyContour

        if (wordTimings.isEmpty() || energyContour.isEmpty()) {
            return Pair(0.5, StressMetrics(0, 0, 0.5, 1.0, "Insufficient data for stress analysis"))
        }

        // Estimate stressed syllables based on energy peaks
        val energyValues = energyContour.map { it.energy }
        val meanEnergy = energyValues.average()
        val highEnergyThreshold = meanEnergy * 1.3

        var stressedCount = 0
        for (word in wordTimings) {
            val wordEnergies = energyContour.filter {
                it.timeSec >= word.startSec && it.timeSec <= word.endSec
            }
            val maxWordEnergy = wordEnergies.maxOfOrNull { it.energy } ?: 0.0
            if (maxWordEnergy > highEnergyThreshold) {
                stressedCount++
            }
        }

        // Estimate expected stressed words (roughly 60-70% for content words)
        val expectedStressCount = (wordTimings.size * 0.65).toInt()

        // Calculate energy contrast ratio
        val maxEnergy = energyValues.maxOrNull() ?: 1.0
        val minEnergy = energyValues.minOrNull() ?: 0.0
        val contrastRatio = if (minEnergy > 0) maxEnergy / minEnergy else 1.0

        // Score based on appropriate stress
        val stressRatio = stressedCount.toDouble() / wordTimings.size
        val score = when {
            stressRatio < 0.3 -> 0.5 // Too flat
            stressRatio in 0.3..0.8 -> 1.0
            else -> 0.7 // Over-stressed
        }

        val interpretation = when {
            stressRatio < 0.3 -> "Too little stress variation, emphasize important words"
            stressRatio in 0.3..0.8 -> "Good stress patterns with appropriate emphasis"
            else -> "Overly stressed, relax pronunciation"
        }

        return Pair(
            score,
            StressMetrics(
                stressedSyllableCount = stressedCount,
                expectedStressCount = expectedStressCount,
                stressPlacementAccuracy = min(1.0, 1.0 - abs(stressedCount - expectedStressCount).toDouble() / expectedStressCount),
                energyContrastRatio = contrastRatio,
                interpretation = interpretation
            )
        )
    }

    /**
     * Score pacing/speech rate.
     * Future: Calibrate optimal ranges per language and proficiency level.
     */
    private fun scorePacing(features: ProsodyFeatures): Pair<Double, PacingMetrics> {
        val wordTimings = features.wordTimings
        val duration = features.duration

        if (wordTimings.isEmpty() || duration == 0.0) {
            return Pair(0.0, PacingMetrics(0.0, 0.0, 0.0, 0.0, "No speech detected"))
        }

        val totalSyllables = wordTimings.sumOf { it.syllableCount }
        val syllablesPerSecond = totalSyllables / duration
        val wordsPerMinute = (wordTimings.size / duration) * 60.0

        // Score based on optimal range
        val pacingScore = when {
            wordsPerMinute < OPTIMAL_WPM_MIN * 0.7 -> 0.4 // Too slow
            wordsPerMinute < OPTIMAL_WPM_MIN -> 0.7
            wordsPerMinute in OPTIMAL_WPM_MIN..OPTIMAL_WPM_MAX -> 1.0
            wordsPerMinute <= OPTIMAL_WPM_MAX * 1.3 -> 0.8
            else -> 0.5 // Too fast
        }

        val interpretation = when {
            wordsPerMinute < OPTIMAL_WPM_MIN -> "Speech is too slow, try to speak more fluently"
            wordsPerMinute in OPTIMAL_WPM_MIN..OPTIMAL_WPM_MAX -> "Appropriate speech rate"
            else -> "Speech is too fast, slow down for clarity"
        }

        return Pair(
            pacingScore,
            PacingMetrics(
                syllablesPerSecond = syllablesPerSecond,
                wordsPerMinute = wordsPerMinute,
                optimalRangeMin = OPTIMAL_WPM_MIN,
                optimalRangeMax = OPTIMAL_WPM_MAX,
                interpretation = interpretation
            )
        )
    }

    /**
     * Score fluency based on pauses and disfluencies.
     * Future: Add detection of repairs, repetitions, and false starts.
     */
    private fun scoreFluency(features: ProsodyFeatures): Pair<Double, FluencyMetrics> {
        val pauses = features.pauseRegions
        val duration = features.duration

        val pauseCount = pauses.size
        val longPauseCount = pauses.count { (it.endSec - it.startSec) > LONG_PAUSE_THRESHOLD_SEC }
        val filledPauseCount = pauses.count { it.filled }
        val avgPauseDuration = if (pauses.isNotEmpty()) {
            pauses.map { it.endSec - it.startSec }.average()
        } else 0.0

        // Calculate disfluency rate (pauses per minute)
        val disfluencyRate = if (duration > 0) (pauseCount / duration) * 60.0 else 0.0

        // Score based on pause characteristics
        val pauseFrequencyScore = when {
            disfluencyRate < 5.0 -> 1.0 // Very fluent
            disfluencyRate in 5.0..15.0 -> 0.9
            disfluencyRate in 15.0..30.0 -> 0.7
            else -> 0.5
        }

        val pauseDurationScore = when {
            avgPauseDuration < 0.3 -> 1.0
            avgPauseDuration in 0.3..0.6 -> 0.8
            else -> 0.6
        }

        val filledPauseScore = when {
            filledPauseCount == 0 -> 1.0
            filledPauseCount <= pauseCount * 0.2 -> 0.8
            else -> 0.6
        }

        val score = (pauseFrequencyScore * 0.4 + pauseDurationScore * 0.3 + filledPauseScore * 0.3)

        val interpretation = when {
            disfluencyRate < 10.0 && filledPauseCount == 0 -> "Excellent fluency with smooth delivery"
            disfluencyRate < 20.0 -> "Good fluency with natural pauses"
            else -> "Choppy delivery with frequent pauses, practice for smoother speech"
        }

        return Pair(
            score,
            FluencyMetrics(
                pauseCount = pauseCount,
                longPauseCount = longPauseCount,
                filledPauseCount = filledPauseCount,
                averagePauseDurationSec = avgPauseDuration,
                disfluencyRate = disfluencyRate,
                interpretation = interpretation
            )
        )
    }

    /**
     * Calculate weighted overall score
     */
    private fun calculateOverallScore(
        rhythm: Double,
        intonation: Double,
        stress: Double,
        pacing: Double,
        fluency: Double
    ): Double {
        return (rhythm * RHYTHM_WEIGHT +
                intonation * INTONATION_WEIGHT +
                stress * STRESS_WEIGHT +
                pacing * PACING_WEIGHT +
                fluency * FLUENCY_WEIGHT)
    }

    /**
     * Generate actionable feedback based on scores
     */
    private fun generateFeedback(
        rhythmResult: Pair<Double, RhythmMetrics>,
        intonationResult: Pair<Double, IntonationMetrics>,
        stressResult: Pair<Double, StressMetrics>,
        pacingResult: Pair<Double, PacingMetrics>,
        fluencyResult: Pair<Double, FluencyMetrics>
    ): List<ProsodyFeedback> {
        val feedback = mutableListOf<ProsodyFeedback>()

        // Rhythm feedback
        if (rhythmResult.first < 0.7) {
            feedback.add(ProsodyFeedback(
                category = ProsodyCategory.RHYTHM,
                severity = if (rhythmResult.first < 0.5) FeedbackSeverity.CRITICAL else FeedbackSeverity.WARNING,
                message = rhythmResult.second.interpretation,
                suggestion = "Practice with a metronome or read along with native speakers"
            ))
        }

        // Intonation feedback
        if (intonationResult.first < 0.7) {
            feedback.add(ProsodyFeedback(
                category = ProsodyCategory.INTONATION,
                severity = if (intonationResult.first < 0.5) FeedbackSeverity.CRITICAL else FeedbackSeverity.WARNING,
                message = intonationResult.second.interpretation,
                suggestion = "Listen to native speech and mimic the pitch patterns, especially at sentence endings"
            ))
        }

        // Stress feedback
        if (stressResult.first < 0.7) {
            feedback.add(ProsodyFeedback(
                category = ProsodyCategory.STRESS,
                severity = FeedbackSeverity.WARNING,
                message = stressResult.second.interpretation,
                suggestion = "Emphasize content words (nouns, verbs, adjectives) more than function words"
            ))
        }

        // Pacing feedback
        if (pacingResult.first < 0.7) {
            feedback.add(ProsodyFeedback(
                category = ProsodyCategory.PACING,
                severity = if (pacingResult.first < 0.5) FeedbackSeverity.CRITICAL else FeedbackSeverity.WARNING,
                message = pacingResult.second.interpretation,
                suggestion = if (pacingResult.second.wordsPerMinute < OPTIMAL_WPM_MIN) {
                    "Practice speaking more quickly and fluently without long pauses"
                } else {
                    "Slow down and articulate each word clearly"
                }
            ))
        }

        // Fluency feedback
        if (fluencyResult.first < 0.7) {
            feedback.add(ProsodyFeedback(
                category = ProsodyCategory.FLUENCY,
                severity = if (fluencyResult.first < 0.5) FeedbackSeverity.CRITICAL else FeedbackSeverity.WARNING,
                message = fluencyResult.second.interpretation,
                suggestion = "Reduce hesitations and filler words. Prepare and practice the text beforehand"
            ))
        }

        // Overall positive feedback
        if (feedback.isEmpty()) {
            feedback.add(ProsodyFeedback(
                category = ProsodyCategory.OVERALL,
                severity = FeedbackSeverity.INFO,
                message = "Excellent prosody across all dimensions",
                suggestion = "Keep up the great work! Focus on maintaining this natural rhythm"
            ))
        }

        return feedback
    }
}
