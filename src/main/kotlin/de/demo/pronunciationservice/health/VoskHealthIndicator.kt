package de.demo.pronunciationservice.health

import de.demo.pronunciationservice.service.VoskService
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class VoskHealthIndicator(
    private val voskService: VoskService
) : HealthIndicator {

    override fun health(): Health {
        val details = mutableMapOf<String, Any>()
        
        return if (voskService.isAvailable()) {
            details["status"] = "Model loaded and ready"
            Health.up().withDetails(details).build()
        } else {
            details["status"] = "Model not configured or failed to load"
            details["note"] = "Configure vosk.model-path to enable Vosk transcription"
            Health.unknown().withDetails(details).build()
        }
    }
}
