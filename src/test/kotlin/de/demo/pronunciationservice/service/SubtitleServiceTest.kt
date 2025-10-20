package de.demo.pronunciationservice.service

import de.demo.pronunciationservice.model.TranscriptSegmentDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SubtitleServiceTest {

    private val subtitleService = SubtitleService()

    @Test
    fun `generateSrt should return empty string for empty segments`() {
        val result = subtitleService.generateSrt(emptyList())
        assertEquals("", result)
    }

    @Test
    fun `generateSrt should format single segment correctly`() {
        val segments = listOf(
            TranscriptSegmentDto(
                text = "Hello world",
                startMs = 0,
                endMs = 2000
            )
        )

        val result = subtitleService.generateSrt(segments)
        
        val expected = """
            |1
            |00:00:00,000 --> 00:00:02,000
            |Hello world
            |
            """.trimMargin()

        assertEquals(expected, result)
    }

    @Test
    fun `generateSrt should format multiple segments correctly`() {
        val segments = listOf(
            TranscriptSegmentDto(
                text = "Hello world",
                startMs = 0,
                endMs = 2000
            ),
            TranscriptSegmentDto(
                text = "This is a test",
                startMs = 2500,
                endMs = 5000
            ),
            TranscriptSegmentDto(
                text = "Subtitle generation",
                startMs = 5500,
                endMs = 8000
            )
        )

        val result = subtitleService.generateSrt(segments)
        
        val expected = """
            |1
            |00:00:00,000 --> 00:00:02,000
            |Hello world
            |
            |2
            |00:00:02,500 --> 00:00:05,000
            |This is a test
            |
            |3
            |00:00:05,500 --> 00:00:08,000
            |Subtitle generation
            |
            """.trimMargin()

        assertEquals(expected, result)
    }

    @Test
    fun `generateSrt should handle timestamps with hours`() {
        val segments = listOf(
            TranscriptSegmentDto(
                text = "Long video content",
                startMs = 3661500, // 1:01:01,500
                endMs = 3665000    // 1:01:05,000
            )
        )

        val result = subtitleService.generateSrt(segments)
        
        val expected = """
            |1
            |01:01:01,500 --> 01:01:05,000
            |Long video content
            |
            """.trimMargin()

        assertEquals(expected, result)
    }

    @Test
    fun `generateSrt should handle precise milliseconds`() {
        val segments = listOf(
            TranscriptSegmentDto(
                text = "Precise timing",
                startMs = 1234,
                endMs = 5678
            )
        )

        val result = subtitleService.generateSrt(segments)
        
        val expected = """
            |1
            |00:00:01,234 --> 00:00:05,678
            |Precise timing
            |
            """.trimMargin()

        assertEquals(expected, result)
    }
}
