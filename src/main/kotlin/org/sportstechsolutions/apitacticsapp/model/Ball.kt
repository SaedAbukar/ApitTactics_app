package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "ball")
data class Ball(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var color: String? = null
)