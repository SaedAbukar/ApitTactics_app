package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "user_group")
data class UserGroup(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    var name: String = "",
    var description: String? = null,

    @ManyToMany
    @JoinTable(
        name = "user_group_member",
        joinColumns = [JoinColumn(name = "group_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    val members: MutableList<User> = mutableListOf()
)