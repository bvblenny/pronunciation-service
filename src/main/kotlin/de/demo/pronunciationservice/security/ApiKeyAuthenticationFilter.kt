package de.demo.pronunciationservice.security

import com.fasterxml.jackson.databind.ObjectMapper
import de.demo.pronunciationservice.config.SecurityProperties
import de.demo.pronunciationservice.model.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class ApiKeyAuthenticationFilter(
    private val securityProperties: SecurityProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter::class.java)

    companion object {
        const val API_KEY_HEADER = "X-API-Key"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestUri = request.requestURI

        // Skip authentication if security is disabled, but still set authentication context
        if (!securityProperties.enabled) {
            // Set authentication so Spring Security doesn't block the request
            val authentication = UsernamePasswordAuthenticationToken(
                "anonymous-user",
                null,
                listOf(SimpleGrantedAuthority("ROLE_API_USER"))
            )
            SecurityContextHolder.getContext().authentication = authentication
            filterChain.doFilter(request, response)
            return
        }

        // Skip authentication for actuator health endpoints
        if (requestUri.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response)
            return
        }

        // Skip authentication for non-API endpoints
        if (!requestUri.startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        // Allow OPTIONS requests (CORS preflight)
        if (request.method == "OPTIONS") {
            filterChain.doFilter(request, response)
            return
        }

        val apiKey = request.getHeader(API_KEY_HEADER)

        if (apiKey.isNullOrBlank()) {
            logger.warn("Missing API key for request: {} {}", request.method, requestUri)
            sendErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Missing API key. Please provide a valid API key in the $API_KEY_HEADER header.",
                requestUri
            )
            return
        }

        if (!isValidApiKey(apiKey)) {
            logger.warn("Invalid API key attempt for request: {} {}", request.method, requestUri)
            sendErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Invalid API key. Please provide a valid API key.",
                requestUri
            )
            return
        }

        // Set authentication in the security context
        val authentication = UsernamePasswordAuthenticationToken(
            "api-user",
            null,
            listOf(SimpleGrantedAuthority("ROLE_API_USER"))
        )
        SecurityContextHolder.getContext().authentication = authentication

        logger.debug("API key validated successfully for request: {} {}", request.method, requestUri)
        filterChain.doFilter(request, response)
    }

    private fun isValidApiKey(apiKey: String): Boolean {
        return securityProperties.apiKeys.contains(apiKey)
    }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        status: HttpStatus,
        error: String,
        message: String,
        path: String
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val errorResponse = ErrorResponse(
            status = status.value(),
            error = error,
            message = message,
            path = path
        )

        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
