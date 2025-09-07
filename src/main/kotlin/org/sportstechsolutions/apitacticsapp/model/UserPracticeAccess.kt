package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "user_practice")
data class UserPracticeAccess(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @ManyToOne
    @JoinColumn(name = "practice_id")
    var practice: Practice? = null,

    @Enumerated(EnumType.STRING)
    var role: AccessRole = AccessRole.VIEWER
)