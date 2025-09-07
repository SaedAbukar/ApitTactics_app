package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "formation_position")
data class FormationPosition(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    @ManyToOne
    @JoinColumn(name = "formation_id")
    var formation: Formation? = null,
    @ManyToOne
    @JoinColumn(name = "team_id")
    var team: Team? = null,
    var x: Double = 0.0,
    var y: Double = 0.0
)