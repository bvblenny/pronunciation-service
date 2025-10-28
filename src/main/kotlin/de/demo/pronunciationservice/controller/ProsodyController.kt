package de.demo.pronunciationservice.controller

import de.demo.pronunciationservice.model.ProsodyScoreDto
import de.demo.pronunciationservice.service.ProsodyFeatureExtractionService
import de.demo.pronunciationservice.service.ProsodyScoringService
import de.demo.pronunciationservice.service.TranscriptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.logging.Logger

@RestController
@RequestMapping("/api/prosody")
@Tag(name = "Prosody Evaluation", description = "Endpoints for suprasegmental (prosody) scoring with explainability")
class ProsodyController(
    private val transcriptionService: TranscriptionService,
    private val featureExtractionService: ProsodyFeatureExtractionService,
    private val scoringService: ProsodyScoringService
) {

    private val logger = Logger.getLogger(ProsodyController::class.java.name)

    /**
     * Evaluate prosody (suprasegmental features) of speech audio.
     * Provides explainable scoring across rhythm, intonation, stress, pacing, and fluency dimensions.
     *
     * @param audioFile The audio file containing speech to be analyzed
     * @param referenceText The expected text for context (optional but recommended)
     * @param languageCode The language code (e.g., "en-US", "de-DE")
     * @return Detailed prosody score with sub-scores, diagnostics, and learner feedback
     */
    @PostMapping("/evaluate", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Evaluate prosody features",
        description = "Analyzes suprasegmental features (rhythm, intonation, stress, pacing, fluency) " +
                "and returns numeric scores with diagnostic metrics and actionable learner feedback. " +
                "Individual scoring components are designed to be replaceable with ML models.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Prosody evaluation completed successfully",
                content = [Content(schema = Schema(implementation = ProsodyScoreDto::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid audio file or parameters")
        ]
    )
    fun evaluateProsody(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("referenceText", required = false, defaultValue = "") referenceText: String,
        @RequestParam("languageCode", defaultValue = "en-US") languageCode: String
    ): ResponseEntity<ProsodyScoreDto> {
        logger.info("Received prosody evaluation request for language: $languageCode")

        if (audioFile.isEmpty) {
            logger.warning("Empty audio file received")
            return ResponseEntity.badRequest().build()
        }

        return try {
            // Normalize audio to standard format
            val wavBytes = transcriptionService.toWavBytes(audioFile)

            // Extract acoustic features
            logger.fine("Extracting prosody features...")
            val features = featureExtractionService.extractFeatures(wavBytes)

            // Score features and generate feedback
            logger.fine("Scoring prosody features...")
            val prosodyScore = scoringService.score(features, referenceText, languageCode)

            logger.info("Prosody evaluation completed. Overall score: ${prosodyScore.overallScore}")
            ResponseEntity.ok(prosodyScore)

        } catch (e: Exception) {
            logger.severe("Error during prosody evaluation: ${e.message}")
            e.printStackTrace()
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * Get prosody features without scoring (useful for debugging or custom analysis).
     *
     * @param audioFile The audio file to analyze
     * @return Raw prosody features extracted from the audio
     */
    @PostMapping("/features", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Extract prosody features only",
        description = "Extracts raw acoustic features (pitch, energy, timing) without scoring. " +
                "Useful for debugging, research, or feeding into custom scoring models."
    )
    fun extractFeatures(
        @RequestParam("audio") audioFile: MultipartFile
    ): ResponseEntity<de.demo.pronunciationservice.model.ProsodyFeatures> {
        logger.info("Received request to extract prosody features")

        if (audioFile.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        return try {
            val wavBytes = transcriptionService.toWavBytes(audioFile)
            val features = featureExtractionService.extractFeatures(wavBytes)

            ResponseEntity.ok(features)
        } catch (e: Exception) {
            logger.severe("Error extracting features: ${e.message}")
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if prosody service is operational")
    fun healthCheck(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "service" to "prosody-evaluation",
            "scorerVersion" to "1.0.0-heuristic",
            "capabilities" to "rhythm,intonation,stress,pacing,fluency"
        )
    }
}

