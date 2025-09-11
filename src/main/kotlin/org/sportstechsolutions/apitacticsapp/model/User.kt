package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "app_user")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    var email: String = "",
    var hashedPassword: String = "",

    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL])
    val sessions: MutableList<Session> = mutableListOf(),

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL])
    val practices: MutableList<Practice> = mutableListOf(),

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL])
    val gameTactics: MutableList<GameTactic> = mutableListOf(),

    @ManyToMany(mappedBy = "members", fetch = FetchType.EAGER)
    val groups: MutableList<UserGroup> = mutableListOf(),

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),
    var lastLogin: Instant? = null
)
