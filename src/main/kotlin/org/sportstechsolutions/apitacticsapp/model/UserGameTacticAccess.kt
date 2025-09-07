package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*
@Entity
@Table(name = "user_gameTactic")
data class UserGameTacticAccess(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @ManyToOne
    @JoinColumn(name = "gameTactic_id")
    var gameTactic: GameTactic? = null,

    @Enumerated(EnumType.STRING)
    var role: AccessRole = AccessRole.VIEWER
)