package de.demo.pronunciationservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Pronunciation Service API")
                .description("REST API for speech transcription and pronunciation evaluation.")
                .version("0.0.1")
                .contact(
                    Contact()
                        .name("Demo Team")
                        .url("https://example.com")
                )
        )
}
