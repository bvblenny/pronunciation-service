package de.demo.pronunciationservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import de.demo.pronunciationservice.security.ApiKeyAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val securityProperties: SecurityProperties,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Allow OPTIONS requests (CORS preflight)
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    // Allow actuator health endpoints
                    .requestMatchers("/actuator/health/**").permitAll()
                    // Allow Swagger/OpenAPI endpoints
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    // Require authentication for all /api/** endpoints
                    .requestMatchers("/api/**").authenticated()
                    // Allow all other requests
                    .anyRequest().permitAll()
            }
            .addFilterBefore(
                ApiKeyAuthenticationFilter(securityProperties, objectMapper),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}
