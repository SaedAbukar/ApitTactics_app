package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*

@Entity
@Table(name = "game_tactic")
data class GameTactic(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    var name: String = "",
    var description: String = "",
    var isPremade: Boolean = false,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var owner: User? = null,

    @OneToMany(mappedBy = "gameTactic", cascade = [CascadeType.ALL], orphanRemoval = true)
    val userAccess: MutableList<UserGameTacticAccess> = mutableListOf(),

    @OneToMany(mappedBy = "gameTactic", cascade = [CascadeType.ALL], orphanRemoval = true)
    val groupAccess: MutableList<GroupGameTacticAccess> = mutableListOf(),

    @ManyToMany(mappedBy = "gameTactics")
    val sessions: MutableList<Session> = mutableListOf()
)
