package de.demo.pronunciationservice.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
class SphinxAlignmentServiceTest {

    @Autowired
    private lateinit var sphinxService: SphinxService

    @Test
    fun `align should return word alignments for a given audio file and transcript`() {

        // GIVEN
        val audioUrl = javaClass.classLoader.getResource("sample-audio.wav")
        assertNotNull(audioUrl, "Test audio file 'sample-audio.wav' not found in resources.")

        val transcript = "hello world"

        // WHEN
        val results = sphinxService.align(audioUrl!!, transcript)

        // THEN
        assertNotNull(results)
        assertEquals(2, results.size, "Should find 2 words in the alignment")
        assertEquals("hello", results[0]?.word)
        assertEquals("world", results[1]?.word)
    }

    @Test
    fun `align should throw exception for non-existent audio file`() {
        // Given
        val nonExistentAudioUrl = java.net.URL("file:///non-existent-audio.wav")
        val transcript = "this will fail"

        // When / Then
        assertThrows(Exception::class.java) {
            sphinxService.align(nonExistentAudioUrl, transcript)
        }
    }
}
