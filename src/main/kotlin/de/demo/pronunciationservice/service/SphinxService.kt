package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.PhonemeEvaluationDto
import de.demo.pronunciationservice.model.RecognizedSpeechDto
import de.demo.pronunciationservice.model.SentenceEvaluationDto
import de.demo.pronunciationservice.model.WordEvaluationDto
import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.SpeechAligner
import edu.cmu.sphinx.api.StreamSpeechRecognizer
import edu.cmu.sphinx.result.WordResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL

/**
 * Thin wrapper around CMU Sphinx for:
 * - forced alignment of a given transcript to an audio file
 * - speech recognition to get a quick transcript and word timings
 */
@Service
class SphinxService(
    @Value("\${sphinx.acoustic-model}") private val acousticModel: String,
    @Value("\${sphinx.dictionary}") private val dictionary: String,
    @Value("\${sphinx.language-model}") private val languageModel: String
) {

    private fun buildConfig(): Configuration = Configuration().apply {
        acousticModelPath = acousticModel
        dictionaryPath = dictionary
        languageModelPath = languageModel
    }

    @Throws(Exception::class)
    fun align(audioUrl: URL, transcript: String): SentenceEvaluationDto {
        val aligner = SpeechAligner(acousticModel, dictionary, null)
        val results = aligner.align(audioUrl, transcript)
        val wordEvaluationResults = results.map { it.toWordEvaluationDto() }
        return SentenceEvaluationDto(transcript = transcript, words = wordEvaluationResults)
    }

    fun recognize(audioBytes: ByteArray): RecognizedSpeechDto {
        val recognizer = StreamSpeechRecognizer(buildConfig())
        recognizer.startRecognition(audioBytes.inputStream())
        val result = recognizer.result
        val transcript = result?.hypothesis ?: ""
        val words = result?.words?.map { it.toWordEvaluationDto() } ?: emptyList()
        recognizer.stopRecognition()
        return RecognizedSpeechDto(
            transcript = transcript,
            words = words
        )
    }

    private fun WordResult.toWordEvaluationDto(): WordEvaluationDto {
        // Extract phoneme-level details if available
        val phonemeResults = mutableListOf<PhonemeEvaluationDto>()
        val pronunciation = this.word.mostLikelyPronunciation
        val phones = pronunciation?.units
        if (phones != null && phones.isNotEmpty()) {
            // Sphinx does not provide direct timing for each phoneme in WordResult,
            // but we can estimate by splitting the word's time frame equally among phonemes
            val wordStart = this.timeFrame.start / 1000.0
            val wordEnd = this.timeFrame.end / 1000.0
            val duration = wordEnd - wordStart
            val phoneCount = phones.size
            val phoneDuration = if (phoneCount > 0) duration / phoneCount else 0.0
            phones.forEachIndexed { idx, unit ->
                val phoneStart = wordStart + idx * phoneDuration
                val phoneEnd = phoneStart + phoneDuration
                phonemeResults.add(
                    PhonemeEvaluationDto(
                        phoneme = unit.name,
                        evaluation = pronunciation.probability.toDouble(),
                        startTime = phoneStart,
                        endTime = phoneEnd
                    )
                )
            }
        }
        return WordEvaluationDto(
            word = this.word.spelling,
            startTime = this.timeFrame.start / 1000.0,
            endTime = this.timeFrame.end / 1000.0,
            evaluation = this.word.mostLikelyPronunciation.probability,
            phonemes = phonemeResults
        )
    }
}
