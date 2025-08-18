package de.demo.pronunciationservice.service

import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1.WordInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class PronunciationScoringServiceTest {

    private lateinit var pronunciationService: PronunciationService

    @Mock
    private lateinit var speechClient: SpeechClient

    @BeforeEach
    fun setUp() {
        pronunciationService = PronunciationService()
        // We'll test the helper methods directly since mocking the Google Cloud client is complex
    }

    @Test
    fun `test calculateTextSimilarity with identical texts`() {
        val actual = "hello world"
        val expected = "hello world"

        val similarity = ReflectionTestUtils.invokeMethod<Double>(
            pronunciationService,
            "calculateTextSimilarity",
            actual,
            expected
        )

        assertEquals(1.0, similarity)
    }

    @Test
    fun `test calculateTextSimilarity with slightly different texts`() {
        val actual = "helo world"  // Missing an 'l'
        val expected = "hello world"

        val similarity = ReflectionTestUtils.invokeMethod<Double>(
            pronunciationService,
            "calculateTextSimilarity",
            actual,
            expected
        )

        // Should be less than 1.0 but still high
        assertTrue(similarity!! < 1.0)
        assertTrue(similarity > 0.8)
    }

    @Test
    fun `test calculateTextSimilarity with very different texts`() {
        val actual = "goodbye everyone"
        val expected = "hello world"

        val similarity = ReflectionTestUtils.invokeMethod<Double>(
            pronunciationService,
            "calculateTextSimilarity",
            actual,
            expected
        )

        // Should be low
        assertTrue(similarity!! < 0.5)
    }

    @Test
    fun `test levenshteinDistance with identical strings`() {
        val s1 = "hello"
        val s2 = "hello"

        val distance = ReflectionTestUtils.invokeMethod<Int>(
            pronunciationService,
            "levenshteinDistance",
            s1,
            s2
        )

        assertEquals(0, distance)
    }

    @Test
    fun `test levenshteinDistance with different strings`() {
        val s1 = "kitten"
        val s2 = "sitting"

        val distance = ReflectionTestUtils.invokeMethod<Int>(
            pronunciationService,
            "levenshteinDistance",
            s1,
            s2
        )

        // The Levenshtein distance between "kitten" and "sitting" is 3
        assertEquals(3, distance)
    }

    @Test
    fun `test extractWordDetails with matching words`() {
        val referenceText = "hello world"

        // Create a mock SpeechRecognitionAlternative with word info
        val wordInfo1 = WordInfo.newBuilder()
            .setWord("hello")
            .setConfidence(0.9f)
            .build()

        val wordInfo2 = WordInfo.newBuilder()
            .setWord("world")
            .setConfidence(0.85f)
            .build()

        val alternative = SpeechRecognitionAlternative.newBuilder()
            .setTranscript("hello world")
            .addWords(wordInfo1)
            .addWords(wordInfo2)
            .build()

        val wordDetails = ReflectionTestUtils.invokeMethod<List<WordDetail>>(
            pronunciationService,
            "extractWordDetails",
            alternative,
            referenceText
        )

        assertEquals(2, wordDetails?.size)
        assertEquals("hello", wordDetails?.get(0)?.word)
        assertEquals(true, wordDetails?.get(0)?.isCorrect)
        assertEquals("world", wordDetails?.get(1)?.word)
        assertEquals(true, wordDetails?.get(1)?.isCorrect)
    }

    @Test
    fun `test extractWordDetails with mismatched words`() {
        val referenceText = "hello world"

        // Create a mock SpeechRecognitionAlternative with word info
        val wordInfo1 = WordInfo.newBuilder()
            .setWord("hello")
            .setConfidence(0.9f)
            .build()

        val wordInfo2 = WordInfo.newBuilder()
            .setWord("earth")  // Different from reference
            .setConfidence(0.7f)
            .build()

        val alternative = SpeechRecognitionAlternative.newBuilder()
            .setTranscript("hello earth")
            .addWords(wordInfo1)
            .addWords(wordInfo2)
            .build()

        val wordDetails = ReflectionTestUtils.invokeMethod<List<WordDetail>>(
            pronunciationService,
            "extractWordDetails",
            alternative,
            referenceText
        )

        assertEquals(2, wordDetails?.size)
        assertEquals("hello", wordDetails?.get(0)?.word)
        assertEquals(true, wordDetails?.get(0)?.isCorrect)
        assertEquals("earth", wordDetails?.get(1)?.word)
        assertEquals(false, wordDetails?.get(1)?.isCorrect)
        assertEquals("world", wordDetails?.get(1)?.expectedWord)
    }
}
