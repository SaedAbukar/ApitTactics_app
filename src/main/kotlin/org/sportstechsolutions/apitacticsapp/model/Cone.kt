package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "cone")
data class Cone(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "step_id")
    var step: Step? = null,

    var x: Int = 0,
    var y: Int = 0,
    var color: String? = null
)