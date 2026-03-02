package org.sportstechsolutions.apitacticsapp.security
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {
    fun getCurrentUserId(): Int? {
        val auth = SecurityContextHolder.getContext().authentication

        // Return null for null auth, unauthenticated requests, or the Anonymous token
        if (auth == null || !auth.isAuthenticated || auth is AnonymousAuthenticationToken) {
            return null
        }

        val principal = auth.principal ?: return null

        return when (principal) {
            is Int -> principal
            is String -> {
                // If the principal isn't numeric (like "anonymousUser"), toIntOrNull returns null
                principal.toIntOrNull()
            }
            else -> null
        }
    }
}
