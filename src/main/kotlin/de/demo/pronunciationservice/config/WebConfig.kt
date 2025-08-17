package de.demo.pronunciationservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val env: Environment) {
    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        val originPatterns = env.getProperty("app.cors.allowed-origin-patterns")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: listOf("http://localhost:4200")

        val allowedMethods = env.getProperty("app.cors.allowed-methods", "GET,POST,PUT,DELETE,OPTIONS")
            .split(",")
            .map { it.trim() }
            .toTypedArray()

        val allowedHeaders = env.getProperty("app.cors.allowed-headers", "*")
            .split(",")
            .map { it.trim() }
            .toTypedArray()

        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedOriginPatterns(*originPatterns.toTypedArray())
                    .allowedMethods(*allowedMethods)
                    .allowedHeaders(*allowedHeaders)
            }
        }
    }
}
