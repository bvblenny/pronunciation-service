package de.demo.pronunciationservice.controller

import de.demo.pronunciationservice.service.PronunciationScore
import de.demo.pronunciationservice.service.PronunciationScoringService
import de.demo.pronunciationservice.service.SphinxService
import de.demo.pronunciationservice.service.WordAlignmentResult
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.logging.Logger


@RestController
@RequestMapping("/api/pronunciation")
class PronunciationController(
    private val pronunciationScoringService: PronunciationScoringService,
    private val sphinxService: SphinxService
) {

    private val logger = Logger.getLogger(PronunciationController::class.java.name)

    /**
     * Endpoint to score pronunciation by comparing audio to reference text
     *
     * @param audio The audio file containing speech to be analyzed
     * @param referenceText The expected text that should be pronounced in the audio
     * @param languageCode The language code (e.g., "en-US", "de-DE")
     * @return A PronunciationScore object with the scoring results
     */
    @PostMapping("/score", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun scorePronunciation(
        @RequestParam("audio") audio: MultipartFile,
        @RequestParam("referenceText") referenceText: String,
        @RequestParam("languageCode", defaultValue = "en-US") languageCode: String
    ): PronunciationScore {
        logger.info("Received pronunciation scoring request for reference text: '$referenceText'")

        if (audio.isEmpty) {
            throw IllegalArgumentException("Audio file cannot be empty")
        }

        val audioBytes = audio.bytes

        return pronunciationScoringService.scorePronunciation(
            audioBytes = audioBytes,
            referenceText = referenceText,
            languageCode = languageCode
        )
    }

    @PostMapping("/align-sphinx")
    fun evaluateWithSphinx(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("referenceText") referenceText: String
    ): ResponseEntity<MutableList<WordAlignmentResult?>> {
        if (audioFile.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        val tempFile: Path?

        try {
            tempFile = Files.createTempFile("sphinx-audio-", audioFile.originalFilename)

            audioFile.inputStream.use { input ->
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING)
            }

            val alignment: MutableList<WordAlignmentResult?> = sphinxService.align(
                tempFile.toUri().toURL(),
                referenceText
            )

            // Hier kannst du die Ergebnisse weiterverarbeiten
            // z.B. Wörter mit sehr niedrigem Score als "schlecht" markieren
            return ResponseEntity.ok(alignment)
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/health")
    fun healthCheck(): Map<String, String> {
        return mapOf("status" to "UP", "service" to "pronunciation-scoring")
    }
}
