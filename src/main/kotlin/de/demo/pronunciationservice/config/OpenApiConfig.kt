package de.demo.pronunciationservice.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Pronunciation Service API")
                .description("REST API for speech transcription and pronunciation evaluation. All API endpoints require authentication via X-API-Key header.")
                .version("0.0.1")
                .contact(
                    Contact()
                        .name("Demo Team")
                        .url("https://example.com")
                )
        )
        .components(
            Components()
                .addSecuritySchemes("ApiKeyAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .`in`(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("API Key for authentication")
                )
        )
        .addSecurityItem(SecurityRequirement().addList("ApiKeyAuth"))
}
