package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*

@Entity
@Table(name = "step")
data class Step(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "session_id")
    var session: Session? = null,

    @OneToMany(
        mappedBy = "step",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val players: MutableList<Player> = mutableListOf(),

    @OneToMany(
        mappedBy = "step",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val balls: MutableList<Ball> = mutableListOf(),

    @OneToMany(
        mappedBy = "step",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val goals: MutableList<Goal> = mutableListOf(),

    @ManyToMany
    @JoinTable(
        name = "step_team",
        joinColumns = [JoinColumn(name = "step_id")],
        inverseJoinColumns = [JoinColumn(name = "team_id")]
    )
    val teams: MutableList<Team> = mutableListOf(),

    @OneToMany(
        mappedBy = "step",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val formations: MutableList<Formation> = mutableListOf(),

    @OneToMany(
        mappedBy = "step",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val cones: MutableList<Cone> = mutableListOf()
)