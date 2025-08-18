package de.demo.pronunciationservice.model

/**
 * DTO representing the response for a transcription request.
 *
 * @property transcript The full text transcript from the transcribed audio or video.
 * @property segments An optional list of transcript segments, where each segment contains
 * specific portions of the transcription along with timing information.
 */
data class TranscriptionResponseDto(
    val transcript: String,
    val segments: List<TranscriptSegmentDto>? = null
)

