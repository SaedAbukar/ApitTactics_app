package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*
@Entity
@Table(name = "user")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    var email: String = "",
    var password: String = "",
    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL])
    val sessions: MutableList<Session> = mutableListOf(),

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL])
    val practices: MutableList<Practice> = mutableListOf(),

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL])
    val gameTactics: MutableList<GameTactic> = mutableListOf()
)