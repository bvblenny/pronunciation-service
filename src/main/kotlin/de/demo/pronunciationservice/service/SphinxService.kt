package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.PhonemeEvaluationResult
import de.demo.pronunciationservice.model.RecognizedSpeechResult
import de.demo.pronunciationservice.model.SentenceEvaluationResult
import de.demo.pronunciationservice.model.WordEvaluationResult
import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.SpeechAligner
import edu.cmu.sphinx.api.StreamSpeechRecognizer
import edu.cmu.sphinx.result.WordResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL


@Service
class SphinxService(
    @Value("\${sphinx.acoustic-model}") private val acousticModel: String,
    @Value("\${sphinx.dictionary}") private val dictionary: String,
    @Value("\${sphinx.language-model}") private val languageModel: String
) {

    private val config = Configuration().apply {
        acousticModelPath = acousticModel
        dictionaryPath = dictionary
        languageModelPath = languageModel
    }

    private val aligner = SpeechAligner(
        this.acousticModel,
        this.dictionary,
        null
    )

    private val speechRecognizer = StreamSpeechRecognizer(config)

    @Synchronized
    @Throws(Exception::class)
    fun align(audioUrl: URL, transcript: String): SentenceEvaluationResult {
        val results = aligner.align(audioUrl, transcript)
        val wordEvaluationResults = results.map { it.toWordEvaluationResult() }

        return SentenceEvaluationResult(transcript = transcript, words = wordEvaluationResults)
    }

    fun recognize(audioBytes: ByteArray): RecognizedSpeechResult {
        speechRecognizer.startRecognition(audioBytes.inputStream())
        val result = speechRecognizer.result
        val transcript = result?.hypothesis ?: ""
        val words = result?.words?.map { it.toWordEvaluationResult() } ?: emptyList()
        speechRecognizer.stopRecognition()
        return RecognizedSpeechResult(
            transcript = transcript,
            words = words
        )
    }

    private fun WordResult.toWordEvaluationResult(): WordEvaluationResult {
        // Extract phoneme-level details if available
        val phonemeResults = mutableListOf<PhonemeEvaluationResult>()
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
                    PhonemeEvaluationResult(
                        phoneme = unit.name,
                        evaluation = pronunciation.probability.toDouble(),
                        startTime = phoneStart,
                        endTime = phoneEnd
                    )
                )
            }
        }
        return WordEvaluationResult(
            word = this.word.spelling,
            startTime = this.timeFrame.start / 1000.0,
            endTime = this.timeFrame.end / 1000.0,
            evaluation = this.word.mostLikelyPronunciation.probability,
            phonemes = phonemeResults
        )
    }

}
