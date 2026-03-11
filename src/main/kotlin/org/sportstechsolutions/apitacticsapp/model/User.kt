package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "app_user")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    @Column(name = "name")
    var name: String? = null,
    var email: String = "",
    var hashedPassword: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var authProvider: AuthProvider = AuthProvider.LOCAL,

    var providerId: String? = null,

    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    @Column(nullable = false)
    var isPublic: Boolean = true,

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
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        if (id != 0 && other.id != 0) return id == other.id
        return email == other.email
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

