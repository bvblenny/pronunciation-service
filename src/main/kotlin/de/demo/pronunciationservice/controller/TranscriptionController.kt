package de.demo.pronunciationservice.controller

import de.demo.pronunciationservice.model.LanguageDto
import de.demo.pronunciationservice.model.SubtitleResponseDto
import de.demo.pronunciationservice.model.TranscriptionResponseDto
import de.demo.pronunciationservice.service.SubtitleService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/transcription")
class TranscriptionController(
    private val transcriptionService: de.demo.pronunciationservice.service.TranscriptionService,
    private val subtitleService: SubtitleService
) {

    @PostMapping(
        "/transcribe",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun transcribe(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("languageCode", defaultValue = "en-US") languageCode: String,
        @RequestParam("provider", defaultValue = "sphinx") provider: String
    ): ResponseEntity<TranscriptionResponseDto> {
        val contentType = audioFile.contentType
        val isAudio = contentType?.startsWith("audio/") == true
        val isVideo = contentType?.startsWith("video/") == true
        val hasKnownExt = (audioFile.originalFilename ?: "").lowercase().let { name ->
            name.endsWith(".mp3") ||
            name.endsWith(".wav") ||
            name.endsWith(".m4a") ||
            name.endsWith(".webm") ||
            name.endsWith(".mp4") ||
            name.endsWith(".mov")
        }
        if (!isAudio && !isVideo && !hasKnownExt) {
            return ResponseEntity.badRequest().build()
        }

        val response = transcriptionService.transcribe(audioFile, languageCode, provider)
        return ResponseEntity.ok(response)
    }

    /**
     * Transcribes audio or video file and generates SRT format subtitles.
     *
     * This endpoint extends the transcription functionality by generating subtitle content
     * in SRT (SubRip Subtitle) format along with the transcription. The timing information
     * from the transcription segments is formatted into standard SRT format suitable for
     * use with media players and video editing software.
     *
     * @param audioFile The audio or video file to transcribe (MP3, WAV, M4A, WebM, MP4, MOV)
     * @param languageCode The language code for transcription (default: en-US)
     * @param provider The transcription provider to use (default: sphinx)
     * @return SubtitleResponseDto containing transcript, segments, and SRT-formatted subtitle content
     */
    @PostMapping(
        "/transcribe-with-subtitles",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun transcribeWithSubtitles(
        @RequestParam("audio") audioFile: MultipartFile,
        @RequestParam("languageCode", defaultValue = "en-US") languageCode: String,
        @RequestParam("provider", defaultValue = "sphinx") provider: String
    ): ResponseEntity<SubtitleResponseDto> {
        val contentType = audioFile.contentType
        val isAudio = contentType?.startsWith("audio/") == true
        val isVideo = contentType?.startsWith("video/") == true
        val hasKnownExt = (audioFile.originalFilename ?: "").lowercase().let { name ->
            name.endsWith(".mp3") ||
            name.endsWith(".wav") ||
            name.endsWith(".m4a") ||
            name.endsWith(".webm") ||
            name.endsWith(".mp4") ||
            name.endsWith(".mov")
        }
        if (!isAudio && !isVideo && !hasKnownExt) {
            return ResponseEntity.badRequest().build()
        }

        val transcriptionResponse = transcriptionService.transcribe(audioFile, languageCode, provider)
        val subtitleContent = subtitleService.generateSrt(transcriptionResponse.segments ?: emptyList())

        val response = SubtitleResponseDto(
            transcript = transcriptionResponse.transcript,
            segments = transcriptionResponse.segments,
            subtitleContent = subtitleContent
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/languages", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun languages(): List<LanguageDto> {
        return listOf(
            LanguageDto("en-US", "English (US)"),
            LanguageDto("en-GB", "English (UK)"),
            LanguageDto("de-DE", "German"),
            LanguageDto("fr-FR", "French"),
            LanguageDto("es-ES", "Spanish")
        )
    }
}
