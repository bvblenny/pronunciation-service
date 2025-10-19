package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.*
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.*

/**
 * Service for extracting acoustic features from audio for prosody analysis.
 * Provides raw features that can be used by different scoring algorithms.
 */
@Service
class ProsodyFeatureExtractionService(
    private val sphinxService: SphinxService
) {

    companion object {
        private const val FRAME_SIZE_MS = 10.0
        private const val PITCH_MIN_HZ = 75.0
        private const val PITCH_MAX_HZ = 500.0
        private const val PAUSE_THRESHOLD_SEC = 0.2
        private val FILLED_PAUSE_WORDS = setOf("um", "uh", "er", "ah", "like", "you know")
    }

    /**
     * Extract all prosody features from audio bytes.
     *
     * @param wavBytes Audio data in WAV format (16kHz, 16-bit, mono)
     * @return Extracted prosody features
     */
    fun extractFeatures(wavBytes: ByteArray): ProsodyFeatures {
        val audioStream = AudioInputStream(
            ByteArrayInputStream(wavBytes),
            AudioSystem.getAudioFileFormat(ByteArrayInputStream(wavBytes)).format,
            wavBytes.size.toLong()
        )

        val format = audioStream.format
        val sampleRate = format.sampleRate
        val frameSize = (sampleRate * FRAME_SIZE_MS / 1000.0).toInt()

        // Extract raw audio samples
        val samples = extractSamples(wavBytes)

        // Extract pitch contour using autocorrelation
        val pitchContour = extractPitchContour(samples, sampleRate.toInt(), frameSize)

        // Extract energy contour
        val energyContour = extractEnergyContour(samples, sampleRate.toInt(), frameSize)

        // Get word timings from Sphinx
        val recognitionResult = sphinxService.recognize(wavBytes)
        val wordTimings = buildWordTimings(recognitionResult.words)

        // Detect pauses
        val pauseRegions = detectPauses(wordTimings, energyContour, sampleRate.toDouble())

        val duration = samples.size / sampleRate.toDouble()

        return ProsodyFeatures(
            duration = duration,
            pitchContour = pitchContour,
            energyContour = energyContour,
            wordTimings = wordTimings,
            pauseRegions = pauseRegions
        )
    }

    /**
     * Extract raw audio samples from WAV bytes
     */
    private fun extractSamples(wavBytes: ByteArray): DoubleArray {
        val audioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(wavBytes))
        val format = audioInputStream.format
        val frameSize = format.frameSize
        val buffer = ByteArray(wavBytes.size)
        val bytesRead = audioInputStream.read(buffer)

        val samples = mutableListOf<Double>()
        val byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN)

        while (byteBuffer.hasRemaining()) {
            if (byteBuffer.remaining() >= 2) {
                val sample = byteBuffer.short.toDouble() / 32768.0 // Normalize to [-1, 1]
                samples.add(sample)
            } else {
                break
            }
        }

        return samples.toDoubleArray()
    }

    /**
     * Extract pitch contour using autocorrelation method
     */
    private fun extractPitchContour(
        samples: DoubleArray,
        sampleRate: Int,
        frameSize: Int
    ): List<PitchPoint> {
        val pitchPoints = mutableListOf<PitchPoint>()
        val hopSize = frameSize / 2

        var frameStart = 0
        while (frameStart + frameSize < samples.size) {
            val frame = samples.sliceArray(frameStart until frameStart + frameSize)
            val timeSec = frameStart.toDouble() / sampleRate

            val (pitch, voiced) = estimatePitch(frame, sampleRate)
            pitchPoints.add(PitchPoint(timeSec, pitch, voiced))

            frameStart += hopSize
        }

        return pitchPoints
    }

    /**
     * Estimate pitch for a single frame using autocorrelation
     */
    private fun estimatePitch(frame: DoubleArray, sampleRate: Int): Pair<Double, Boolean> {
        val minLag = (sampleRate / PITCH_MAX_HZ).toInt()
        val maxLag = (sampleRate / PITCH_MIN_HZ).toInt()

        // Calculate autocorrelation
        var maxCorrelation = 0.0
        var bestLag = minLag

        for (lag in minLag..min(maxLag, frame.size - 1)) {
            var correlation = 0.0
            for (i in 0 until frame.size - lag) {
                correlation += frame[i] * frame[i + lag]
            }

            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestLag = lag
            }
        }

        // Calculate energy to determine if voiced
        val energy = frame.map { it * it }.sum()
        val voiced = energy > 0.01 && maxCorrelation > 0.3 * energy

        val pitch = if (voiced) sampleRate.toDouble() / bestLag else 0.0

        return Pair(pitch, voiced)
    }

    /**
     * Extract energy contour (RMS energy per frame)
     */
    private fun extractEnergyContour(
        samples: DoubleArray,
        sampleRate: Int,
        frameSize: Int
    ): List<EnergyPoint> {
        val energyPoints = mutableListOf<EnergyPoint>()
        val hopSize = frameSize / 2

        var frameStart = 0
        while (frameStart + frameSize < samples.size) {
            val frame = samples.sliceArray(frameStart until frameStart + frameSize)
            val timeSec = frameStart.toDouble() / sampleRate

            // Calculate RMS energy
            val energy = sqrt(frame.map { it * it }.average())
            energyPoints.add(EnergyPoint(timeSec, energy))

            frameStart += hopSize
        }

        return energyPoints
    }

    /**
     * Build word timing information with syllable estimation
     */
    private fun buildWordTimings(words: List<WordEvaluationDto>): List<WordTiming> {
        return words.map { word ->
            WordTiming(
                word = word.word,
                startSec = word.startTime,
                endSec = word.endTime,
                syllableCount = estimateSyllableCount(word.word),
                stressed = false // Will be determined by stress analyzer
            )
        }
    }

    /**
     * Estimate syllable count using vowel counting heuristic
     */
    private fun estimateSyllableCount(word: String): Int {
        val vowels = "aeiouy"
        var count = 0
        var previousWasVowel = false

        for (char in word.lowercase()) {
            val isVowel = char in vowels
            if (isVowel && !previousWasVowel) {
                count++
            }
            previousWasVowel = isVowel
        }

        // Adjust for silent e
        if (word.lowercase().endsWith("e") && count > 1) {
            count--
        }

        return max(1, count)
    }

    /**
     * Detect pause regions based on word gaps and energy
     */
    private fun detectPauses(
        wordTimings: List<WordTiming>,
        @Suppress("UNUSED_PARAMETER") energyContour: List<EnergyPoint>,
        @Suppress("UNUSED_PARAMETER") sampleRate: Double
    ): List<PauseRegion> {
        val pauses = mutableListOf<PauseRegion>()

        // Detect pauses between words
        for (i in 0 until wordTimings.size - 1) {
            val currentWord = wordTimings[i]
            val nextWord = wordTimings[i + 1]
            val gap = nextWord.startSec - currentWord.endSec

            if (gap > PAUSE_THRESHOLD_SEC) {
                val isFilled = nextWord.word.lowercase() in FILLED_PAUSE_WORDS
                pauses.add(PauseRegion(currentWord.endSec, nextWord.startSec, isFilled))
            }
        }

        return pauses
    }
}
