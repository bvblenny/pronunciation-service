package de.demo.pronunciationservice.service

import edu.cmu.sphinx.api.SpeechAligner
import edu.cmu.sphinx.linguist.dictionary.Pronunciation
import edu.cmu.sphinx.result.WordResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL
import java.util.stream.Collectors


@Service
class SphinxService(
    @Value("\${sphinx.acoustic-model}") private val acousticModel: String,
    @Value("\${sphinx.dictionary}") private val dictionary: String,
    @Value("\${sphinx.language-model}") private val languageModel: String
) {

    private val aligner = SpeechAligner(
        this.acousticModel,
        this.dictionary,
        null
    )

    @Synchronized
    @Throws(Exception::class)
    fun align(audioUrl: URL, transcript: String): MutableList<WordAlignmentResult?> {
        val results: MutableList<WordResult?> = aligner.align(audioUrl, transcript)

        return results.stream()
            .map { wordResult: WordResult? -> WordAlignmentResult(wordResult!!) }
            .collect(Collectors.toList())
    }
}

class WordAlignmentResult(wordResult: WordResult) {
    val word: String? = wordResult.word.spelling
    val startTime: Long = wordResult.timeFrame.start
    val endTime: Long = wordResult.timeFrame.end
    val score: Double = wordResult.score
    val pronunciation: Pronunciation = wordResult.pronunciation
}
