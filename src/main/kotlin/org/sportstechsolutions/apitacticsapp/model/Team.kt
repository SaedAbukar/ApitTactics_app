package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(
    name = "team",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "name"])]
)
data class Team(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    var name: String = "",
    var color: String = "",

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    var owner: User
)
