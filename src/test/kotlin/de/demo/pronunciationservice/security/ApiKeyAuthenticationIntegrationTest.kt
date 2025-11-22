package de.demo.pronunciationservice.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "app.security.enabled=true",
    "app.security.api-keys=test-api-key-123,another-valid-key"
])
class ApiKeyAuthenticationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    companion object {
        const val VALID_API_KEY = "test-api-key-123"
        const val INVALID_API_KEY = "invalid-key"
    }

    @Test
    fun `should return 401 when API key is missing for API endpoints`() {
        mockMvc.perform(get("/api/transcription/languages"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Missing API key. Please provide a valid API key in the X-API-Key header."))
    }

    @Test
    fun `should return 403 when API key is invalid`() {
        mockMvc.perform(get("/api/transcription/languages")
            .header("X-API-Key", INVALID_API_KEY))
            .andExpect(status().isForbidden)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"))
            .andExpect(jsonPath("$.message").value("Invalid API key. Please provide a valid API key."))
    }

    @Test
    fun `should return 200 when API key is valid`() {
        mockMvc.perform(get("/api/transcription/languages")
            .header("X-API-Key", VALID_API_KEY))
            .andExpect(status().isOk)
    }

    @Test
    fun `should allow access to actuator health endpoints without API key`() {
        // Health endpoints should be accessible without authentication
        // Should not return 401 or 403 (authentication/authorization errors)
        val result = mockMvc.perform(get("/actuator/health"))
            .andReturn()
        
        val status = result.response.status
        assert(status != 401 && status != 403) { 
            "Expected health endpoint to be accessible without auth, but got status: $status" 
        }
    }

    @Test
    fun `should allow access to actuator health liveness endpoint without API key`() {
        // Liveness probe should be accessible without authentication
        val result = mockMvc.perform(get("/actuator/health/liveness"))
            .andReturn()
        
        val status = result.response.status
        assert(status != 401 && status != 403) { 
            "Expected liveness endpoint to be accessible without auth, but got status: $status" 
        }
    }

    @Test
    fun `should allow access to actuator health readiness endpoint without API key`() {
        // Readiness probe should be accessible without authentication
        val result = mockMvc.perform(get("/actuator/health/readiness"))
            .andReturn()
        
        val status = result.response.status
        assert(status != 401 && status != 403) { 
            "Expected readiness endpoint to be accessible without auth, but got status: $status" 
        }
    }

    @Test
    fun `should allow access to swagger UI without API key`() {
        // Swagger UI should redirect or be accessible without authentication
        val result = mockMvc.perform(get("/swagger-ui.html"))
            .andReturn()
        
        val status = result.response.status
        assert(status != 401 && status != 403) { 
            "Expected Swagger UI to be accessible without auth, but got status: $status" 
        }
    }

    @Test
    fun `should allow OPTIONS requests without API key`() {
        // OPTIONS requests (CORS preflight) should be allowed without authentication
        val result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/transcription/languages"))
            .andReturn()
        
        val status = result.response.status
        assert(status != 401 && status != 403) { 
            "Expected OPTIONS request to be allowed without auth, but got status: $status" 
        }
    }

    @Test
    fun `should accept valid API key for POST endpoints`() {
        val mockFile = MockMultipartFile(
            "audio",
            "test.wav",
            "audio/wav",
            "test audio content".toByteArray()
        )

        // With valid API key, request should not be rejected due to authentication
        val result = mockMvc.perform(multipart("/api/transcription/transcribe")
            .file(mockFile)
            .param("languageCode", "en-US")
            .header("X-API-Key", VALID_API_KEY))
            .andReturn()
        
        val status = result.response.status
        assert(status != 401 && status != 403) { 
            "Expected request with valid API key to not be rejected for auth reasons, but got status: $status" 
        }
    }

    @Test
    fun `should reject POST endpoints without API key`() {
        val mockFile = MockMultipartFile(
            "audio",
            "test.wav",
            "audio/wav",
            "test audio content".toByteArray()
        )

        mockMvc.perform(multipart("/api/transcription/transcribe")
            .file(mockFile)
            .param("languageCode", "en-US"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should accept second valid API key`() {
        mockMvc.perform(get("/api/transcription/languages")
            .header("X-API-Key", "another-valid-key"))
            .andExpect(status().isOk)
    }
}
