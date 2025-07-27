package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.SentenceEvaluationResult
import de.demo.pronunciationservice.model.WordEvaluationResult
import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.SpeechAligner
import edu.cmu.sphinx.api.SpeechResult
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

    fun recognize(audioBytes: ByteArray): SpeechResult {
        speechRecognizer.startRecognition(audioBytes.inputStream())
        val result = speechRecognizer.result

        println("############# SpeechResult: $result #############")
        println("############# Result: ${result.result} #############")

        speechRecognizer.stopRecognition()
        return result
    }

    private fun WordResult.toWordEvaluationResult(): WordEvaluationResult {

        val tmp = this
        val wordEvaluationResult = WordEvaluationResult(
            word = this.word.spelling,
            startTime = this.timeFrame.start / 1000.0,
            endTime = this.timeFrame.end / 1000.0,
            evaluation = this.word.mostLikelyPronunciation.probability, // TODO: fix bug: double value becomes 0.0
            phonemes = emptyList() // TODO: Implement phoneme extraction
        )

        return wordEvaluationResult
    }

}
