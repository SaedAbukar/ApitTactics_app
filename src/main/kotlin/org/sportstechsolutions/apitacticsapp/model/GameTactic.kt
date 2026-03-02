package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*

@Entity
@Table(name = "game_tactic")
class GameTactic(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
    var name: String?,
    var description: String?,

    @Column(name = "is_premade", nullable = false) var isPremade: Boolean = false,
    @Column(name = "is_public", nullable = false) var isPublic: Boolean = false, // Added for Community Sharing
    @Column(name = "view_count", nullable = false) var viewCount: Int = 0,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_favorite_game_tactic",
        joinColumns = [JoinColumn(name = "game_tactic_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    var favoritedByUsers: MutableSet<User> = mutableSetOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var owner: User? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "session_game_tactic",
        joinColumns = [JoinColumn(name = "game_tactic_id")],
        inverseJoinColumns = [JoinColumn(name = "session_id")]
    )
    var sessions: MutableSet<Session> = mutableSetOf(),

    @OneToMany(mappedBy = "gameTactic", cascade = [CascadeType.ALL], orphanRemoval = true)
    var userAccess: MutableSet<UserGameTacticAccess> = mutableSetOf(),

    @OneToMany(mappedBy = "gameTactic", cascade = [CascadeType.ALL], orphanRemoval = true)
    var groupAccess: MutableSet<GroupGameTacticAccess> = mutableSetOf()
)