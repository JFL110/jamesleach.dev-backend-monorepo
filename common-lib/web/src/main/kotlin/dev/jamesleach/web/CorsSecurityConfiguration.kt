package dev.jamesleach.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * TODO merge with AllowAllCorsFilter
 */
@Configuration
class CorsSecurityConfiguration {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("**")
        configuration.allowedMethods = listOf("GET", "POST", "OPTIONS", "DELETE", "PUT", "PATCH")
        configuration.allowedHeaders = listOf("Content-Type", "Accept", "X-Requested-With", "remember-me")
        configuration.allowCredentials = true
        configuration.maxAge = 3600
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}