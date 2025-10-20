package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.TranscriptSegmentDto
import org.springframework.stereotype.Service

/**
 * Service for generating subtitle files from transcription segments.
 */
@Service
class SubtitleService {

    /**
     * Generates an SRT (SubRip Subtitle) formatted string from transcription segments.
     *
     * SRT format specification:
     * 1. Sequence number (starting from 1)
     * 2. Timecode: start --> end (format: HH:MM:SS,mmm)
     * 3. Subtitle text
     * 4. Blank line
     *
     * @param segments List of transcription segments with text and timing information
     * @return SRT formatted string
     */
    fun generateSrt(segments: List<TranscriptSegmentDto>): String {
        if (segments.isEmpty()) {
            return ""
        }

        return segments.mapIndexed { index, segment ->
            val sequenceNumber = index + 1
            val startTime = formatSrtTimestamp(segment.startMs)
            val endTime = formatSrtTimestamp(segment.endMs)
            
            """
            |$sequenceNumber
            |$startTime --> $endTime
            |${segment.text}
            |
            """.trimMargin()
        }.joinToString("")
    }

    /**
     * Formats milliseconds to SRT timestamp format (HH:MM:SS,mmm).
     *
     * @param milliseconds Time in milliseconds
     * @return Formatted timestamp string
     */
    private fun formatSrtTimestamp(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        val seconds = (milliseconds % 60000) / 1000
        val millis = milliseconds % 1000

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
