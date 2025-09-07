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
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "step_id")
    val players: MutableList<Player> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "step_id")
    val balls: MutableList<Ball> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "step_id")
    val goals: MutableList<Goal> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "step_id")
    val teams: MutableList<Team> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "step_id")
    val formations: MutableList<Formation> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "step_id")
    val cones: MutableList<Cone> = mutableListOf()
)