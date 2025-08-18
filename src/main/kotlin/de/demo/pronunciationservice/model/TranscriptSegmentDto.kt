package de.demo.pronunciationservice.model

/**
 * DTO representing a segment of transcribed text within a larger transcription.
 *
 * @property text The transcribed text for this segment.
 * @property startMs The starting timestamp of the segment in milliseconds.
 * @property endMs The ending timestamp of the segment in milliseconds.
 */
data class TranscriptSegmentDto(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

