package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*

@Entity
@Table(name = "practice")
data class Practice(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    var name: String = "",
    var description: String = "",
    var is_premade: Boolean = false,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var owner: User? = null,

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "session_practice",
        joinColumns = [JoinColumn(name = "practice_id")],
        inverseJoinColumns = [JoinColumn(name = "session_id")]
    )
    val sessions: MutableList<Session> = mutableListOf()
)


