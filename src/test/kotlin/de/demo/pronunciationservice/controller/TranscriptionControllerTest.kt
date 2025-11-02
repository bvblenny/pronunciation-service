package de.demo.pronunciationservice.controller

import de.demo.pronunciationservice.model.SubtitleResponseDto
import de.demo.pronunciationservice.model.TranscriptSegmentDto
import de.demo.pronunciationservice.model.TranscriptionResponseDto
import de.demo.pronunciationservice.service.SubtitleService
import de.demo.pronunciationservice.service.TranscriptionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(TranscriptionController::class)
class TranscriptionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var transcriptionService: TranscriptionService

    @MockBean
    private lateinit var subtitleService: SubtitleService

    @Test
    fun `transcribeWithSubtitles should return subtitle response with SRT content`() {
        // Arrange
        val mockFile = MockMultipartFile(
            "file",
            "test.mp3",
            "audio/mpeg",
            "test audio content".toByteArray()
        )

        val segments = listOf(
            TranscriptSegmentDto("Hello", 0, 1000),
            TranscriptSegmentDto("world", 1000, 2000)
        )

        val transcriptionResponse = TranscriptionResponseDto(
            transcript = "Hello world",
            segments = segments
        )

        val srtContent = """
            |1
            |00:00:00,000 --> 00:00:01,000
            |Hello
            |
            |2
            |00:00:01,000 --> 00:00:02,000
            |world
            |
        """.trimMargin()

        `when`(transcriptionService.transcribe(mockFile, "en-US", "sphinx"))
            .thenReturn(transcriptionResponse)
        `when`(subtitleService.generateSrt(segments))
            .thenReturn(srtContent)

        // Act & Assert
        mockMvc.perform(
            multipart("/api/transcription/transcribe-with-subtitles")
                .file(mockFile)
                .param("languageCode", "en-US")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.transcript").value("Hello world"))
            .andExpect(jsonPath("$.segments").isArray)
            .andExpect(jsonPath("$.segments[0].text").value("Hello"))
            .andExpect(jsonPath("$.segments[1].text").value("world"))
            .andExpect(jsonPath("$.subtitleContent").exists())
    }

    @Test
    fun `transcribeWithSubtitles should return bad request for non-media files`() {
        // Arrange
        val mockFile = MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".toByteArray()
        )

        // Act & Assert
        mockMvc.perform(
            multipart("/api/transcription/transcribe-with-subtitles")
                .file(mockFile)
                .param("languageCode", "en-US")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `transcribeWithSubtitles should accept video files`() {
        // Arrange
        val mockFile = MockMultipartFile(
            "file",
            "test.mp4",
            "video/mp4",
            "test video content".toByteArray()
        )

        val segments = listOf(
            TranscriptSegmentDto("Video test", 0, 2000)
        )

        val transcriptionResponse = TranscriptionResponseDto(
            transcript = "Video test",
            segments = segments
        )

        val srtContent = """
            |1
            |00:00:00,000 --> 00:00:02,000
            |Video test
            |
        """.trimMargin()

        `when`(transcriptionService.transcribe(mockFile, "en-US", "sphinx"))
            .thenReturn(transcriptionResponse)
        `when`(subtitleService.generateSrt(segments))
            .thenReturn(srtContent)

        // Act & Assert
        mockMvc.perform(
            multipart("/api/transcription/transcribe-with-subtitles")
                .file(mockFile)
                .param("languageCode", "en-US")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transcript").value("Video test"))
    }

    @Test
    fun `transcribeWithSubtitles should use default language code`() {
        // Arrange
        val mockFile = MockMultipartFile(
            "file",
            "test.wav",
            "audio/wav",
            "test audio content".toByteArray()
        )

        val segments = listOf(
            TranscriptSegmentDto("Test", 0, 1000)
        )

        val transcriptionResponse = TranscriptionResponseDto(
            transcript = "Test",
            segments = segments
        )

        val srtContent = """
            |1
            |00:00:00,000 --> 00:00:01,000
            |Test
            |
        """.trimMargin()

        `when`(transcriptionService.transcribe(mockFile, "en-US", "sphinx"))
            .thenReturn(transcriptionResponse)
        `when`(subtitleService.generateSrt(segments))
            .thenReturn(srtContent)

        // Act & Assert
        mockMvc.perform(
            multipart("/api/transcription/transcribe-with-subtitles")
                .file(mockFile)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transcript").value("Test"))
    }
}
