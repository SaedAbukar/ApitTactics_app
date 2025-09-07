package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "group_gameTactic")
data class GroupGameTacticAccess(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "group_id")
    var group: UserGroup? = null,

    @ManyToOne
    @JoinColumn(name = "gameTactic_id")
    var gameTactic: GameTactic? = null,

    @Enumerated(EnumType.STRING)
    var role: AccessRole = AccessRole.VIEWER
)