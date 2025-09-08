package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*

@Entity
@Table(name = "session")
data class Session(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    var name: String = "",
    var description: String = "",

    @ManyToOne
    @JoinColumn(name = "user_id")
    var owner: User? = null,

    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true)
    val steps: MutableList<Step> = mutableListOf(),

    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true)
    val userAccess: MutableList<UserSessionAccess> = mutableListOf(),

    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true)
    val groupAccess: MutableList<GroupSessionAccess> = mutableListOf(),

    @ManyToMany
    @JoinTable(
        name = "session_practice",
        joinColumns = [JoinColumn(name = "session_id")],
        inverseJoinColumns = [JoinColumn(name = "practice_id")]
    )
    val practices: MutableList<Practice> = mutableListOf(),

    @ManyToMany
    @JoinTable(
        name = "session_game_tactic",
        joinColumns = [JoinColumn(name = "session_id")],
        inverseJoinColumns = [JoinColumn(name = "game_tactic_id")]
    )
    val gameTactics: MutableList<GameTactic> = mutableListOf()
)
