package de.demo.pronunciationservice.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class SphinxHealthIndicator(
    @Value("\${sphinx.acoustic-model}") private val acousticModel: String,
    @Value("\${sphinx.dictionary}") private val dictionary: String,
    @Value("\${sphinx.language-model}") private val languageModel: String
) : HealthIndicator {

    override fun health(): Health {
        val details = mutableMapOf<String, Any>()
        val missing = mutableListOf<String>()

        fun resolve(path: String): Boolean {
            return if (path.startsWith("resource:")) {
                val resPath = path.removePrefix("resource:")
                // Ensure leading slash for classpath lookup
                val normalized = if (resPath.startsWith("/")) resPath else "/$resPath"
                this::class.java.getResource(normalized) != null
            } else {
                java.nio.file.Files.exists(java.nio.file.Path.of(path))
            }
        }

        if (!resolve(acousticModel)) missing.add("acousticModel")
        if (!resolve(dictionary)) missing.add("dictionary")
        if (!resolve(languageModel)) missing.add("languageModel")

        return if (missing.isEmpty()) {
            details["acousticModel"] = okValue(acousticModel)
            details["dictionary"] = okValue(dictionary)
            details["languageModel"] = okValue(languageModel)
            Health.up().withDetails(details).build()
        } else {
            details["missing"] = missing
            Health.down().withDetails(details).build()
        }
    }

    private fun okValue(value: String): Any =
        if (value.length > 64) value.take(61) + "..." else value
}

