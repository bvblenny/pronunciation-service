package de.demo.pronunciationservice.model

/**
 * DTO representing the response for a transcription request with subtitle generation.
 *
 * @property transcript The full text transcript from the transcribed audio or video.
 * @property segments An optional list of transcript segments, where each segment contains
 * specific portions of the transcription along with timing information.
 * @property subtitleContent The subtitle file content in SRT format.
 */
data class SubtitleResponseDto(
    val transcript: String,
    val segments: List<TranscriptSegmentDto>? = null,
    val subtitleContent: String
)
