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

    @ManyToMany(mappedBy = "sessions")
    val practices: MutableList<Practice> = mutableListOf(),

    @ManyToMany(mappedBy = "sessions")
    val gameTactics: MutableList<GameTactic> = mutableListOf()
)