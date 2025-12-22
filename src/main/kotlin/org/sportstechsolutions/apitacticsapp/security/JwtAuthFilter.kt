package org.sportstechsolutions.apitacticsapp.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
): OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // CRITICAL: Remove "Bearer " (7 characters) to get just the token
            val token = authHeader.substring(7)

            try {
                if (jwtService.validateAccessToken(token)) {
                    val userId = jwtService.getUserIdFromToken(token)

                    // Create the Auth object
                    val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())

                    // Link the request details (IP, Session, etc.)
                    auth.details = WebAuthenticationDetailsSource().buildDetails(request)

                    // Set the security context
                    SecurityContextHolder.getContext().authentication = auth
                }
            } catch (e: Exception) {
                // This prevents the 500 Error. It logs the problem and
                // simply fails to authenticate the user for this request.
                println("JWT Auth Error: ${e.message}")
                SecurityContextHolder.clearContext()
            }
        }

        // Always continue the filter chain
        filterChain.doFilter(request, response)
    }
}