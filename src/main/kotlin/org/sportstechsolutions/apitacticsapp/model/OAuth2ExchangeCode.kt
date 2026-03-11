package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "oauth2_exchange_codes")
class OAuth2ExchangeCode(
    @Id val id: String,
    @Column(nullable = false) val userId: Int,
    @Column(nullable = false) val expiresAt: Instant
)