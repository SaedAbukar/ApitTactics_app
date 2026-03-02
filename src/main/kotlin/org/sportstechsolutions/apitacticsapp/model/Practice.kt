package org.sportstechsolutions.apitacticsapp.model

import jakarta.persistence.*

@Entity
@Table(name = "practice")
class Practice(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
    var name: String? = null,
    var description: String? = null,

    @Column(name = "is_premade", nullable = false) var isPremade: Boolean = false,
    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = false,
    @Column(name = "view_count", nullable = false) var viewCount: Int = 0,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_favorite_practice", joinColumns = [JoinColumn(name = "practice_id")], inverseJoinColumns = [JoinColumn(name = "user_id")])
    var favoritedByUsers: MutableSet<User> = mutableSetOf(),

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") var owner: User? = null,

    @Enumerated(EnumType.STRING) @Column(name = "phase_of_play") var phaseOfPlay: PhaseOfPlay? = null,
    @Enumerated(EnumType.STRING) @Column(name = "ball_context") var ballContext: BallContext? = null,
    @Enumerated(EnumType.STRING) @Column(name = "drill_format") var drillFormat: DrillFormat? = null,

    @Column(name = "min_players") var minPlayers: Int? = null,
    @Column(name = "max_players") var maxPlayers: Int? = null,
    @Column(name = "duration_minutes") var durationMinutes: Int? = null,
    @Column(name = "area_size") var areaSize: String? = null,
    @Column(name = "target_age_level") var targetAgeLevel: String? = null,

    @ElementCollection(targetClass = TacticalAction::class)
    @CollectionTable(name = "practice_tactical_action", joinColumns = [JoinColumn(name = "practice_id")])
    @Enumerated(EnumType.STRING) @Column(name = "tactical_action")
    var tacticalActions: MutableSet<TacticalAction> = mutableSetOf(),

    @ElementCollection(targetClass = QualityMaker::class)
    @CollectionTable(name = "practice_quality_maker", joinColumns = [JoinColumn(name = "practice_id")])
    @Enumerated(EnumType.STRING) @Column(name = "quality_maker")
    var qualityMakers: MutableSet<QualityMaker> = mutableSetOf(),

    @ManyToMany(mappedBy = "practices", fetch = FetchType.LAZY)
    var sessions: MutableSet<Session> = mutableSetOf(),

    // --- ADDED FOR REPOSITORY ACCESS CHECKS ---
    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], orphanRemoval = true)
    var userAccess: MutableSet<UserPracticeAccess> = mutableSetOf(),

    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], orphanRemoval = true)
    var groupAccess: MutableSet<GroupPracticeAccess> = mutableSetOf()
)