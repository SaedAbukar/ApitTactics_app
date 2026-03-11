package org.sportstechsolutions.apitacticsapp.security.oauth2

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomUserDetails(
    val id: Int,
    private val email: String,
    private val attributes: Map<String, Any>
) : OAuth2User {
    override fun getName(): String = id.toString()
    override fun getAttributes(): Map<String, Any> = attributes
    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_USER"))
}