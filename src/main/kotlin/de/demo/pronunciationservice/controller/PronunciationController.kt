package de.demo.pronunciationservice.controller

import de.demo.pronunciationservice.model.PronunciationScoreDto
import de.demo.pronunciationservice.model.DetailedAnalysisDto
import de.demo.pronunciationservice.service.PronunciationService
import de.demo.pronunciationservice.service.SphinxService
import de.demo.pronunciationservice.service.TranscriptionService
import de.demo.pronunciationservice.service.DetailedAnalysisService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.util.logging.Logger


@RestController
@RequestMapping("/api/pronunciation")
class PronunciationController(
    private val pronunciationService: PronunciationService,
    private val sphinxService: SphinxService,
    private val transcriptionService: TranscriptionService,
    private val detailedAnalysisService: DetailedAnalysisService
) {

    private val logger = Logger.getLogger(PronunciationController::class.java.name)

    /**
     * Endpoint to score pronunciation by comparing audio to a reference text
     *
     * @param audioFile The audio file containing speech to be analyzed
     * @param referenceText The expected text that should be pronounced in the audio
     * @param languageCode The language code (e.g., "en-US", "de-DE")
     * @return A PronunciationScore object with the scoring results
     */
    @PostMapping("/evaluate-stt", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun evaluateSpeechToText(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("referenceText") referenceText: String,
        @RequestParam("languageCode", defaultValue = "en-US") languageCode: String
    ): ResponseEntity<PronunciationScoreDto> {
        logger.info("Received pronunciation scoring request for reference text: '$referenceText'")

        if (audioFile.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        val wavBytes = transcriptionService.toWavBytes(audioFile)

        val evaluationResult = pronunciationService.evaluate(
            audioBytes = wavBytes,
            referenceText = referenceText,
            languageCode = languageCode
        )

        return ResponseEntity.ok(evaluationResult)
    }

    @PostMapping("/evaluate-sphinx-alignment")
    fun evaluateSphinxAlignment(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("referenceText") referenceText: String,
        @RequestParam("languageCode", defaultValue = "en-US") languageCode: String
    ): ResponseEntity<out Any?> {
        if (audioFile.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        val wavBytes = transcriptionService.toWavBytes(audioFile)
        val tempFile = Files.createTempFile("align_", ".wav")
        try {
            Files.write(tempFile, wavBytes)
            val alignmentResult = sphinxService.align(
                tempFile.toUri().toURL(),
                referenceText
            )
            return ResponseEntity.ok(alignmentResult)
        } finally {
            try { Files.deleteIfExists(tempFile) } catch (_: Exception) {}
        }
    }

    @Suppress("UnusedParameter")
    @PostMapping("/evaluate-sphinx-recognition")
    fun evaluateSphinxRecognition(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("referenceText") referenceText: String,
        @RequestParam("languageCode", defaultValue = "en-US") languageCode: String
    ): ResponseEntity<out Any?> {
        if (audioFile.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        val wavBytes = transcriptionService.toWavBytes(audioFile)
        val recognitionResult = sphinxService.recognize(wavBytes)
        return ResponseEntity.ok(recognitionResult)
    }

    /**
     * Endpoint for detailed analysis of pronunciation using WAV normalization
     *
     * @param audioFile The audio file containing speech to be analyzed
     * @param referenceText The expected text that should be pronounced in the audio
     * @param languageCode The language code (e.g., "en-US", "de-DE")
     * @return A DetailedAnalysisDto object with the detailed analysis results
     */
    @PostMapping("/analyze-detailed", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun analyzeDetailed(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("referenceText") referenceText: String,
        @RequestParam("languageCode", defaultValue = "en-US") languageCode: String
    ): ResponseEntity<DetailedAnalysisDto> {
        if (audioFile.isEmpty) return ResponseEntity.badRequest().build()
        val wavBytes = transcriptionService.toWavBytes(audioFile)
        val analysis = detailedAnalysisService.analyzeDetailed(wavBytes, referenceText)
        return ResponseEntity.ok(analysis)
    }

    @GetMapping("/health")
    fun healthCheck(): Map<String, String> {
        return mapOf("status" to "UP", "service" to "pronunciation-service")
    }
}
