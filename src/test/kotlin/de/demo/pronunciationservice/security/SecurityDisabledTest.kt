package de.demo.pronunciationservice.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "app.security.enabled=false"
])
class SecurityDisabledTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should allow access to API endpoints when security is disabled`() {
        // When security is disabled, API endpoints should be accessible without authentication
        val result = mockMvc.perform(get("/api/transcription/languages"))
            .andReturn()
        
        val status = result.response.status
        assert(status != 401 && status != 403) { 
            "Expected API endpoint to be accessible when security is disabled, but got status: $status" 
        }
    }

    @Test
    fun `should still allow access to health endpoints when security is disabled`() {
        // Health endpoints should always be accessible
        val result = mockMvc.perform(get("/actuator/health"))
            .andReturn()
        
        val status = result.response.status
        assert(status != 401 && status != 403) { 
            "Expected health endpoint to be accessible, but got status: $status" 
        }
    }
}
