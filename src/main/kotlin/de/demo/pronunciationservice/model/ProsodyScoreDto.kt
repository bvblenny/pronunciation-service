package de.demo.pronunciationservice.model

/**
 * Complete prosody evaluation result with numeric scores, diagnostics, and learner feedback.
 * Designed for extensibility - individual scorers can be replaced without breaking the API contract.
 *
 * @property overallScore Aggregate prosody score (0.0-1.0)
 * @property subScores Individual prosody dimension scores
 * @property diagnostics Detailed metrics explaining the scores
 * @property feedback Actionable hints for the learner
 * @property features Extracted acoustic features used for scoring
 * @property metadata Information about the scoring process (model version, etc.)
 */
data class ProsodyScoreDto(
    val overallScore: Double,
    val subScores: ProsodySubScores,
    val diagnostics: ProsodyDiagnostics,
    val feedback: List<ProsodyFeedback>,
    val features: ProsodyFeatures,
    val metadata: ProsodyMetadata
)

/**
 * Individual prosody dimension scores, each independently replaceable.
 *
 * @property rhythm Score for rhythm/timing quality (0.0-1.0)
 * @property intonation Score for pitch variation and contour (0.0-1.0)
 * @property stress Score for word/syllable stress patterns (0.0-1.0)
 * @property pacing Score for speech rate appropriateness (0.0-1.0)
 * @property fluency Score for smoothness and continuity (0.0-1.0)
 */
data class ProsodySubScores(
    val rhythm: Double,
    val intonation: Double,
    val stress: Double,
    val pacing: Double,
    val fluency: Double
)

/**
 * Detailed diagnostic metrics that explain how scores were computed.
 *
 * @property rhythmMetrics Metrics related to rhythm analysis
 * @property intonationMetrics Metrics related to pitch analysis
 * @property stressMetrics Metrics related to stress patterns
 * @property pacingMetrics Metrics related to speech rate
 * @property fluencyMetrics Metrics related to fluency analysis
 */
data class ProsodyDiagnostics(
    val rhythmMetrics: RhythmMetrics,
    val intonationMetrics: IntonationMetrics,
    val stressMetrics: StressMetrics,
    val pacingMetrics: PacingMetrics,
    val fluencyMetrics: FluencyMetrics
)

data class RhythmMetrics(
    val syllableTimingVariance: Double,
    val expectedVariance: Double,
    val isochronyIndex: Double,
    val interpretation: String
)

data class IntonationMetrics(
    val pitchRangeHz: Double,
    val pitchVariationCoefficient: Double,
    val meanPitchHz: Double,
    val contourSmoothness: Double,
    val interpretation: String
)

data class StressMetrics(
    val stressedSyllableCount: Int,
    val expectedStressCount: Int,
    val stressPlacementAccuracy: Double,
    val energyContrastRatio: Double,
    val interpretation: String
)

data class PacingMetrics(
    val syllablesPerSecond: Double,
    val wordsPerMinute: Double,
    val optimalRangeMin: Double,
    val optimalRangeMax: Double,
    val interpretation: String
)

data class FluencyMetrics(
    val pauseCount: Int,
    val longPauseCount: Int,
    val filledPauseCount: Int,
    val averagePauseDurationSec: Double,
    val disfluencyRate: Double,
    val interpretation: String
)

/**
 * Actionable feedback for the learner with severity levels.
 *
 * @property category The prosody dimension this feedback relates to
 * @property severity How critical this issue is (INFO, WARNING, CRITICAL)
 * @property message Human-readable feedback message
 * @property suggestion Specific action the learner can take
 */
data class ProsodyFeedback(
    val category: ProsodyCategory,
    val severity: FeedbackSeverity,
    val message: String,
    val suggestion: String
)

enum class ProsodyCategory {
    RHYTHM,
    INTONATION,
    STRESS,
    PACING,
    FLUENCY,
    OVERALL
}

enum class FeedbackSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Raw acoustic features extracted from the audio.
 * These can be used by future ML models or different scoring algorithms.
 *
 * @property duration Total duration in seconds
 * @property pitchContour Pitch values over time (Hz)
 * @property energyContour Energy/intensity values over time
 * @property wordTimings Word-level timing information
 * @property pauseRegions Detected pause regions
 */
data class ProsodyFeatures(
    val duration: Double,
    val pitchContour: List<PitchPoint>,
    val energyContour: List<EnergyPoint>,
    val wordTimings: List<WordTiming>,
    val pauseRegions: List<PauseRegion>
)

data class PitchPoint(
    val timeSec: Double,
    val frequencyHz: Double,
    val voiced: Boolean
)

data class EnergyPoint(
    val timeSec: Double,
    val energy: Double
)

data class WordTiming(
    val word: String,
    val startSec: Double,
    val endSec: Double,
    val syllableCount: Int,
    val stressed: Boolean
)

data class PauseRegion(
    val startSec: Double,
    val endSec: Double,
    val filled: Boolean // true if filled pause (um, uh)
)

/**
 * Metadata about the scoring process for versioning and debugging.
 *
 * @property scorerVersion Version identifier for the scoring algorithm
 * @property modelType Type of model used (HEURISTIC, CALIBRATED, ML)
 * @property referenceLanguage Language/accent reference used
 * @property processingTimestamp When the analysis was performed
 */
data class ProsodyMetadata(
    val scorerVersion: String,
    val modelType: ModelType,
    val referenceLanguage: String,
    val processingTimestamp: Long
)

enum class ModelType {
    HEURISTIC,
    CALIBRATED,
    ML_BASED
}

