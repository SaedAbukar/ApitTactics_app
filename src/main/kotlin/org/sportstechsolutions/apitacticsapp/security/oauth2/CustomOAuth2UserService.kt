package org.sportstechsolutions.apitacticsapp.security.oauth2

import org.slf4j.LoggerFactory
import org.sportstechsolutions.apitacticsapp.exception.ConflictException
import org.sportstechsolutions.apitacticsapp.exception.UnauthenticatedException
import org.sportstechsolutions.apitacticsapp.model.AuthProvider
import org.sportstechsolutions.apitacticsapp.model.User
import org.sportstechsolutions.apitacticsapp.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository
) : DefaultOAuth2UserService() {

    private val log = LoggerFactory.getLogger(CustomOAuth2UserService::class.java)

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)

        val registrationId = userRequest.clientRegistration.registrationId.uppercase()
        val provider = AuthProvider.valueOf(registrationId)

        log.info("Processing Native OAuth2 login callback for provider: $provider")

        val email = oAuth2User.attributes["email"]?.toString()
            ?: run {
                log.warn("OAuth2 user data from $provider did not contain an email address.")
                throw UnauthenticatedException("Email not found from OAuth2 provider")
            }

        val providerId = oAuth2User.name

        // ---> EXTRACT THE FIRST NAME <---
        val firstName = oAuth2User.attributes["given_name"]?.toString()
            ?: oAuth2User.attributes["first_name"]?.toString()
            ?: oAuth2User.attributes["name"]?.toString()

        val user = resolveUser(provider, providerId, email, firstName)
        user.lastLogin = Instant.now()

        log.info("Successfully loaded OAuth2 user data for internal User ID: ${user.id}")
        return CustomUserDetails(user.id, user.email, oAuth2User.attributes)
    }

    private fun resolveUser(provider: AuthProvider, providerId: String, email: String, name: String?): User {
        log.debug("Resolving user in database for provider $provider and email $email")

        userRepository.findByProviderIdAndAuthProvider(providerId, provider)?.let {
            // Update their name if they changed it on Google
            if (name != null && it.name != name) {
                log.debug("Updating name for existing User ID: ${it.id}")
                it.name = name
            }
            return it
        }

        val existingByEmail = userRepository.findByEmail(email)
        if (existingByEmail != null) {
            if (existingByEmail.authProvider == AuthProvider.LOCAL) {
                log.warn("OAuth conflict: Email $email already registered locally with a password.")
                throw ConflictException("An account with this email already exists using a password.")
            } else if (existingByEmail.authProvider != provider) {
                log.warn("OAuth conflict: Email $email already registered via ${existingByEmail.authProvider}.")
                throw ConflictException("An account with this email already exists via ${existingByEmail.authProvider}.")
            }
            log.info("Linking new provider ID to existing ${existingByEmail.authProvider} account for User ID: ${existingByEmail.id}")
            existingByEmail.providerId = providerId

            // Add name if they didn't have one before
            if (existingByEmail.name == null) {
                existingByEmail.name = name
            }
            return existingByEmail
        }

        log.info("Registering new user via Native OAuth2 provider: $provider")
        return userRepository.save(
            User(email = email, name = name, authProvider = provider, providerId = providerId)
        )
    }
}