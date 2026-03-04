package org.sportstechsolutions.apitacticsapp.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    // This allows us to forward filter errors to your GlobalExceptionHandler!
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver
): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authHeader = request.getHeader("Authorization")

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7)

                if (jwtService.validateAccessToken(token)) {
                    val userId = jwtService.getUserIdFromToken(token)

                    val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                    auth.details = WebAuthenticationDetailsSource().buildDetails(request)

                    SecurityContextHolder.getContext().authentication = auth
                }
            }
            filterChain.doFilter(request, response)

        } catch (e: Exception) {
            SecurityContextHolder.clearContext()

            // Bridge the exception to your GlobalExceptionHandler so it returns your ApiError JSON
            resolver.resolveException(request, response, null, UnauthenticatedException("Invalid or expired access token."))
        }
    }
}