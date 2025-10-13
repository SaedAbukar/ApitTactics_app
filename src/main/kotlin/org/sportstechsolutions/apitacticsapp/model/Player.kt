package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "player")
data class Player(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "step_id")
    var step: Step? = null,

    var x: Int = 0,
    var y: Int = 0,
    var number: Int = 0,
    var color: String = "",

    @ManyToOne
    @JoinColumn(name = "team_id")
    var team: Team? = null
)