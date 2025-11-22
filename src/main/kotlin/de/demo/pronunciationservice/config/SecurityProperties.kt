package de.demo.pronunciationservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.security")
class SecurityProperties {
    var enabled: Boolean = true
    var apiKeys: List<String> = emptyList()
}
