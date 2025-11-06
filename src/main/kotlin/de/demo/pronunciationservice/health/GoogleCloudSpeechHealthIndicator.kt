package de.demo.pronunciationservice.health

import de.demo.pronunciationservice.service.GoogleCloudSpeechService
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class GoogleCloudSpeechHealthIndicator(
    private val googleCloudSpeechService: GoogleCloudSpeechService
) : HealthIndicator {

    override fun health(): Health {
        val details = mutableMapOf<String, Any>()
        
        return if (googleCloudSpeechService.isAvailable()) {
            details["status"] = "Google Cloud Speech-to-Text client initialized and ready"
            details["note"] = "Cloud-based transcription available"
            Health.up().withDetails(details).build()
        } else {
            details["status"] = "Client not initialized or credentials not configured"
            details["note"] = "Configure GOOGLE_APPLICATION_CREDENTIALS or spring.cloud.gcp.credentials.location to enable"
            Health.unknown().withDetails(details).build()
        }
    }
}
