package org.sportstechsolutions.apitacticsapp.security

import org.sportstechsolutions.apitacticsapp.model.User
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {
    fun getCurrentUserId(): Int {
        val principal = SecurityContextHolder.getContext().authentication?.principal
            ?: throw IllegalStateException("No authentication in context")
        return when (principal) {
            is Int -> principal
            is String -> principal.toInt()
            else -> throw IllegalStateException("Invalid principal type")
        }
    }
}
