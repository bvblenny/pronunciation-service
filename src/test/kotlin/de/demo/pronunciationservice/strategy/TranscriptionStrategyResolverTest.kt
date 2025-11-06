package de.demo.pronunciationservice.strategy

import de.demo.pronunciationservice.model.RecognizedSpeechDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TranscriptionStrategyResolverTest {

    @Test
    fun `resolver should find strategy by exact provider name`() {
        val mockStrategy = object : TranscriptionStrategy {
            override fun getProviderNames() = listOf("test-provider")
            override fun isAvailable() = true
            override fun transcribe(audioBytes: ByteArray, languageCode: String) = 
                RecognizedSpeechDto("", emptyList())
        }
        
        val resolver = TranscriptionStrategyResolver(listOf(mockStrategy))
        
        val resolved = resolver.resolve("test-provider")
        assertSame(mockStrategy, resolved)
    }

    @Test
    fun `resolver should find strategy by alias`() {
        val mockStrategy = object : TranscriptionStrategy {
            override fun getProviderNames() = listOf("provider1", "provider-alias", "p1")
            override fun isAvailable() = true
            override fun transcribe(audioBytes: ByteArray, languageCode: String) = 
                RecognizedSpeechDto("", emptyList())
        }
        
        val resolver = TranscriptionStrategyResolver(listOf(mockStrategy))
        
        val resolved1 = resolver.resolve("provider1")
        val resolved2 = resolver.resolve("provider-alias")
        val resolved3 = resolver.resolve("p1")
        
        assertSame(mockStrategy, resolved1)
        assertSame(mockStrategy, resolved2)
        assertSame(mockStrategy, resolved3)
    }

    @Test
    fun `resolver should be case-insensitive`() {
        val mockStrategy = object : TranscriptionStrategy {
            override fun getProviderNames() = listOf("TestProvider")
            override fun isAvailable() = true
            override fun transcribe(audioBytes: ByteArray, languageCode: String) = 
                RecognizedSpeechDto("", emptyList())
        }
        
        val resolver = TranscriptionStrategyResolver(listOf(mockStrategy))
        
        val resolved1 = resolver.resolve("testprovider")
        val resolved2 = resolver.resolve("TESTPROVIDER")
        val resolved3 = resolver.resolve("TestProvider")
        
        assertSame(mockStrategy, resolved1)
        assertSame(mockStrategy, resolved2)
        assertSame(mockStrategy, resolved3)
    }

    @Test
    fun `resolver should throw exception for unknown provider`() {
        val resolver = TranscriptionStrategyResolver(emptyList())
        
        val exception = assertThrows<IllegalArgumentException> {
            resolver.resolve("unknown-provider")
        }
        
        assertTrue(exception.message!!.contains("Unknown transcription provider"))
        assertTrue(exception.message!!.contains("unknown-provider"))
    }

    @Test
    fun `resolver should list all available providers`() {
        val strategy1 = object : TranscriptionStrategy {
            override fun getProviderNames() = listOf("sphinx")
            override fun isAvailable() = true
            override fun transcribe(audioBytes: ByteArray, languageCode: String) = 
                RecognizedSpeechDto("", emptyList())
        }
        
        val strategy2 = object : TranscriptionStrategy {
            override fun getProviderNames() = listOf("vosk")
            override fun isAvailable() = true
            override fun transcribe(audioBytes: ByteArray, languageCode: String) = 
                RecognizedSpeechDto("", emptyList())
        }
        
        val strategy3 = object : TranscriptionStrategy {
            override fun getProviderNames() = listOf("google", "gcp", "google-cloud")
            override fun isAvailable() = true
            override fun transcribe(audioBytes: ByteArray, languageCode: String) = 
                RecognizedSpeechDto("", emptyList())
        }
        
        val resolver = TranscriptionStrategyResolver(listOf(strategy1, strategy2, strategy3))
        
        val providers = resolver.getAvailableProviders()
        
        assertEquals(5, providers.size)
        assertTrue(providers.contains("sphinx"))
        assertTrue(providers.contains("vosk"))
        assertTrue(providers.contains("google"))
        assertTrue(providers.contains("gcp"))
        assertTrue(providers.contains("google-cloud"))
    }

    @Test
    fun `resolver should handle multiple strategies`() {
        val sphinxStrategy = object : TranscriptionStrategy {
            override fun getProviderNames() = listOf("sphinx")
            override fun isAvailable() = true
            override fun transcribe(audioBytes: ByteArray, languageCode: String) = 
                RecognizedSpeechDto("sphinx", emptyList())
        }
        
        val voskStrategy = object : TranscriptionStrategy {
            override fun getProviderNames() = listOf("vosk")
            override fun isAvailable() = true
            override fun transcribe(audioBytes: ByteArray, languageCode: String) = 
                RecognizedSpeechDto("vosk", emptyList())
        }
        
        val resolver = TranscriptionStrategyResolver(listOf(sphinxStrategy, voskStrategy))
        
        val resolvedSphinx = resolver.resolve("sphinx")
        val resolvedVosk = resolver.resolve("vosk")
        
        assertSame(sphinxStrategy, resolvedSphinx)
        assertSame(voskStrategy, resolvedVosk)
    }
}
