package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "group_game_tactic") // ✅ renamed
data class GroupGameTacticAccess(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "group_id")
    var group: UserGroup? = null,

    @ManyToOne
    @JoinColumn(name = "game_tactic_id")
    var gameTactic: GameTactic? = null,

    @Enumerated(EnumType.STRING)
    var role: AccessRole = AccessRole.VIEWER
)