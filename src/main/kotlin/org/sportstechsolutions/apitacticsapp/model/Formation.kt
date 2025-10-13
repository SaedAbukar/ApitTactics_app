package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*

@Entity
@Table(name = "formation")
data class Formation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "step_id")
    var step: Step? = null,

    var name: String = "",

    @OneToMany(
        mappedBy = "formation",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val positions: MutableList<FormationPosition> = mutableListOf()
)