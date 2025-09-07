package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "group_practice")
data class GroupPracticeAccess(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "group_id")
    var group: UserGroup? = null,

    @ManyToOne
    @JoinColumn(name = "practice_id")
    var practice: Practice? = null,

    @Enumerated(EnumType.STRING)
    var role: AccessRole = AccessRole.VIEWER
)