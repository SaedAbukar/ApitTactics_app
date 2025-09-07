package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "user_session")
data class UserSessionAccess(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @ManyToOne
    @JoinColumn(name = "session_id")
    var session: Session? = null,

    @Enumerated(EnumType.STRING)
    var role: AccessRole = AccessRole.VIEWER
)