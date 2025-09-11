package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    val userId: Int,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false, unique = true, length = 256)
    val hashedToken: String,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
