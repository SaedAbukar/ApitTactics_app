package org.sportstechsolutions.apitacticsapp.security.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.model.OAuth2ExchangeCode
import org.sportstechsolutions.apitacticsapp.repository.OAuth2ExchangeCodeRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.util.UUID

@Component
class OAuth2AuthenticationSuccessHandler(
    private val authorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository,
    private val exchangeCodeRepository: OAuth2ExchangeCodeRepository,
    @Value("\${app.oauth2.authorized-redirect-uri}") private val redirectUri: String
) : SimpleUrlAuthenticationSuccessHandler() {

    private val log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler::class.java)

    override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        val userPrincipal = authentication.principal as CustomUserDetails

        log.info("OAuth2 provider authentication successful for User ID: ${userPrincipal.id}. Generating secure exchange code.")

        val exchangeCode = UUID.randomUUID().toString()

        exchangeCodeRepository.save(
            OAuth2ExchangeCode(
                id = exchangeCode,
                userId = userPrincipal.id,
                expiresAt = Instant.now().plusSeconds(60)
            )
        )

        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response)

        val targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("code", exchangeCode)
            .build().toUriString()

        if (!response.isCommitted) {
            log.debug("Redirecting user to frontend with one-time exchange code.")
            redirectStrategy.sendRedirect(request, response, targetUrl)
        }
    }
}