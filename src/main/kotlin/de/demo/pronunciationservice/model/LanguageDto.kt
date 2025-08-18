package de.demo.pronunciationservice.model

/**
 * DTO representing a language supported by the transcription service.
 *
 * @property code The language code, combined with a regional code (e.g., "en-US", "de-DE").
 * @property name The human-readable name of the language (e.g., "English (US)", "German").
 */
data class LanguageDto(
    val code: String,
    val name: String
)
