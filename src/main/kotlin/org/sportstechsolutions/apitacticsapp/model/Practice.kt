package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*

@Entity
@Table(name = "practice")
data class Practice(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    var name: String = "",
    var description: String = "",
    var is_premade: Boolean = false, // ✅ snake_case

    @ManyToOne
    @JoinColumn(name = "user_id")
    var owner: User? = null,

    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], orphanRemoval = true)
    val userAccess: MutableList<UserPracticeAccess> = mutableListOf(),

    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], orphanRemoval = true)
    val groupAccess: MutableList<GroupPracticeAccess> = mutableListOf(),

    @ManyToMany(mappedBy = "practices")
    val sessions: MutableList<Session> = mutableListOf()
)
