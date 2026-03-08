package org.sportstechsolutions.apitacticsapp.security

import jakarta.servlet.DispatcherType
import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.servlet.HandlerExceptionResolver

@Configuration
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: List<String>
) {
    private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        log.info("Configuring SecurityFilterChain")
        return http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/").permitAll()
                    .requestMatchers(HttpMethod.POST, "/sessions/search").permitAll()
                    .requestMatchers(HttpMethod.POST, "/practices/search").permitAll()
                    .requestMatchers(HttpMethod.POST, "/game-tactics/search").permitAll()
                    .requestMatchers(HttpMethod.GET, "/sessions/{id}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/practices/{id}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/game-tactics/{id}").permitAll()
                    .requestMatchers("/auth/**").permitAll()
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { configurer ->
                configurer.authenticationEntryPoint { request, response, _ ->
                    log.warn("Unauthorized access attempt to ${request.requestURI}")
                    resolver.resolveException(
                        request, response, null,
                        UnauthenticatedException("Authentication is required to access this resource.")
                    )
                }
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Use the injected variable instead of hardcoded strings
        configuration.allowedOrigins = allowedOrigins
        configuration.allowedMethods = listOf("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}