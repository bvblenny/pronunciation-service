package de.demo.pronunciationservice.strategy

import org.springframework.stereotype.Component

/**
 * Resolver for transcription strategies.
 * 
 * Manages the collection of available transcription strategies and resolves
 * the appropriate strategy based on the provider name.
 * 
 * This follows the Strategy pattern where strategies are registered and
 * selected at runtime based on configuration or request parameters.
 */
@Component
class TranscriptionStrategyResolver(
    strategies: List<TranscriptionStrategy>
) {
    
    // Map provider names to their corresponding strategies
    private val strategyMap: Map<String, TranscriptionStrategy> = strategies
        .flatMap { strategy -> 
            strategy.getProviderNames().map { name -> name.lowercase() to strategy }
        }
        .toMap()
    
    /**
     * Resolves a transcription strategy by provider name.
     * 
     * @param providerName The name of the provider (case-insensitive)
     * @return The corresponding TranscriptionStrategy
     * @throws IllegalArgumentException if the provider is not found
     */
    fun resolve(providerName: String): TranscriptionStrategy {
        val strategy = strategyMap[providerName.lowercase()]
        
        return strategy 
            ?: throw IllegalArgumentException(
                "Unknown transcription provider: $providerName. " +
                "Available providers: ${strategyMap.keys.sorted().joinToString(", ")}"
            )
    }
    
    /**
     * Returns all available provider names.
     */
    fun getAvailableProviders(): List<String> {
        return strategyMap.keys.sorted()
    }
}
