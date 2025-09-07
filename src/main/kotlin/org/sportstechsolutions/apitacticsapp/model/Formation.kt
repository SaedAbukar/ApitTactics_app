package org.sportstechsolutions.apitacticsapp.model
import jakarta.persistence.*

@Entity
@Table(name = "formation")
data class Formation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    var name: String = "",
    @OneToMany(mappedBy = "formation", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val positions: MutableList<FormationPosition> = mutableListOf()
)