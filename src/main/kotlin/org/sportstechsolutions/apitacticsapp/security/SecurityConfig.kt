package org.sportstechsolutions.apitacticsapp.security

import jakarta.servlet.DispatcherType
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.springframework.beans.factory.annotation.Qualifier
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
    // Inject the resolver so we can forward security errors to the GlobalExceptionHandler
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .cors { it.configurationSource(corsConfigurationSource()) } // enable CORS
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/").permitAll()

                    // FIXED: Allow guests to hit the search endpoints across all 3 domains
                    .requestMatchers(HttpMethod.POST, "/sessions/search").permitAll()
                    .requestMatchers(HttpMethod.POST, "/practices/search").permitAll()
                    .requestMatchers(HttpMethod.POST, "/game-tactics/search").permitAll()

                    // FIXED: Allow guests to view specific public items
                    .requestMatchers(HttpMethod.GET, "/sessions/{id}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/practices/{id}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/game-tactics/{id}").permitAll()

                    .requestMatchers("/auth/**").permitAll()
                    .dispatcherTypeMatchers(
                        DispatcherType.ERROR,
                        DispatcherType.FORWARD
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { configurer ->
                // FIXED: Replace the blank 401 response with your standardized ApiError JSON
                configurer.authenticationEntryPoint { request, response, _ ->
                    resolver.resolveException(
                        request,
                        response,
                        null,
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
        configuration.allowedOrigins = listOf(
            "http://localhost:5173",
            "http://172.20.10.2:5173",
            "https://tacticflow-client.onrender.com"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}