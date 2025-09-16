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
 * Service for speech recognition and alignment using CMU Sphinx.
 * Provides methods to recognize speech from audio and align it with a given transcript.
 *
 * @property acousticModel Path to the acoustic model used by Sphinx.
 * @property dictionary Path to the dictionary used by Sphinx.
 * @property languageModel Path to the language model used by Sphinx.
 */
@Service
class SphinxService(
    @Value("\${sphinx.acoustic-model}") private val acousticModel: String,
    @Value("\${sphinx.dictionary}") private val dictionary: String,
    @Value("\${sphinx.language-model}") private val languageModel: String
) {

    /**
     * Builds the Sphinx configuration using the provided model, dictionary, and language paths.
     *
     * @return A [Configuration] object configured for speech recognition.
     */
    private fun buildConfig(): Configuration = Configuration().apply {
        acousticModelPath = acousticModel
        dictionaryPath = dictionary
        languageModelPath = languageModel
    }

    /**
     * Aligns the given audio with the provided transcript using the SpeechAligner.
     *
     * @param audioUrl The URL of the audio file to align.
     * @param transcript The transcript to align the audio against.
     * @return A [SentenceEvaluationDto] containing the alignment results.
     * @throws Exception If an error occurs during alignment.
     */
    @Throws(Exception::class)
    fun align(audioUrl: URL, transcript: String): SentenceEvaluationDto {
        val aligner = SpeechAligner(acousticModel, dictionary, null)
        val results = aligner.align(audioUrl, transcript)
        val wordEvaluationResults = results.map { it.toWordEvaluationDto() }

        return SentenceEvaluationDto(transcript = transcript, words = wordEvaluationResults)
    }

    /**
     * Recognizes speech from the given audio byte array.
     *
     * @param audioBytes The audio data as a byte array.
     * @return A [RecognizedSpeechDto] containing the recognized transcript and word details.
     */
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

    /**
     * Converts a [WordResult] from Sphinx into a [WordEvaluationDto].
     * Extracts phoneme-level details and estimates phoneme timings.
     *
     * @return A [WordEvaluationDto] representing the word and its evaluation details.
     */
    private fun WordResult.toWordEvaluationDto(): WordEvaluationDto {
        val phonemeResults = mutableListOf<PhonemeEvaluationDto>()
        val pronunciation = this.word.mostLikelyPronunciation
        val phones = pronunciation?.units
        if (phones != null && phones.isNotEmpty()) {
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
