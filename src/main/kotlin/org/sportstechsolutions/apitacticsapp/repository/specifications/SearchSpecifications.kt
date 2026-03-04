package org.sportstechsolutions.apitacticsapp.repository.specifications

import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.sportstechsolutions.apitacticsapp.dtos.*
import org.sportstechsolutions.apitacticsapp.model.*
import org.springframework.data.jpa.domain.Specification

object SearchSpecifications {

    // -------------------------------------------------------------------
    // PRACTICE SEARCH SPECIFICATION
    // -------------------------------------------------------------------
    fun buildPracticeSearchSpec(request: PracticeSearchRequest, userId: Int): Specification<Practice> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            val isPublic = cb.isTrue(root.get("isPublic"))
            val isPremade = cb.isTrue(root.get("isPremade"))
            val publicOrPremade = cb.or(isPublic, isPremade)

            // >> DB-Level Private Access Check (Replaces in-memory accessibleIds) <<
            val privateAccess = if (userId != 0 && query != null) {
                // Check direct user access
                val userSq = query.subquery(Int::class.java)
                val userSqRoot = userSq.from(UserPracticeAccess::class.java)
                userSq.select(cb.literal(1)).where(
                    cb.equal(userSqRoot.get<Practice>("practice"), root),
                    cb.equal(userSqRoot.join<Any, Any>("user").get<Int>("id"), userId)
                )

                // Check group access
                val groupSq = query.subquery(Int::class.java)
                val groupSqRoot = groupSq.from(GroupPracticeAccess::class.java)
                groupSq.select(cb.literal(1)).where(
                    cb.equal(groupSqRoot.get<Practice>("practice"), root),
                    cb.equal(groupSqRoot.join<Any, Any>("group").join<Any, Any>("members").get<Int>("id"), userId)
                )

                cb.or(cb.exists(userSq), cb.exists(groupSq))
            } else {
                cb.disjunction()
            }

            // SCOPE FILTERING
            when (request.searchScope) {
                SearchScope.MY_ITEMS ->
                    predicates.add(cb.equal(root.get<User>("owner").get<Int>("id"), userId))

                SearchScope.PREMADE ->
                    predicates.add(isPremade)

                SearchScope.PUBLIC_COMMUNITY -> {
                    predicates.add(isPublic)
                    predicates.add(cb.isFalse(isPremade))
                    if (userId != 0) predicates.add(cb.notEqual(root.get<User>("owner").get<Int>("id"), userId))
                }

                SearchScope.SHARED_WITH_ME -> {
                    predicates.add(privateAccess)
                    if (userId != 0) predicates.add(cb.notEqual(root.get<User>("owner").get<Int>("id"), userId))
                    predicates.add(cb.isFalse(isPremade))
                    predicates.add(cb.isFalse(isPublic))
                }

                SearchScope.ALL_ACCESSIBLE -> {
                    // Added isOwner check so users can see their own private items in a general search
                    val isOwner = cb.equal(root.get<User>("owner").get<Int>("id"), userId)
                    predicates.add(cb.or(privateAccess, publicOrPremade, isOwner))
                }
            }

            // TAXONOMY & FILTERS
            if (request.onlyFavorites && userId != 0) {
                predicates.add(cb.equal(root.join<Practice, User>("favoritedByUsers").get<Int>("id"), userId))
            }

            request.searchTerm?.takeIf { it.isNotBlank() }?.let { term ->
                val p = "%${term.lowercase()}%"
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), p), cb.like(cb.lower(root.get("description")), p)))
            }

            request.phaseOfPlay?.let { predicates.add(cb.equal(root.get<Any>("phaseOfPlay"), it)) }
            request.ballContext?.let { predicates.add(cb.equal(root.get<Any>("ballContext"), it)) }
            request.drillFormat?.let { predicates.add(cb.equal(root.get<Any>("drillFormat"), it)) }
            request.targetAgeLevel?.let { predicates.add(cb.equal(root.get<String>("targetAgeLevel"), it)) }

            if (!request.tacticalActions.isNullOrEmpty()) {
                predicates.add(root.join<Practice, Any>("tacticalActions", JoinType.LEFT).`in`(request.tacticalActions))
            }
            if (!request.qualityMakers.isNullOrEmpty()) {
                predicates.add(root.join<Practice, Any>("qualityMakers", JoinType.LEFT).`in`(request.qualityMakers))
            }

            query?.distinct(true)
            cb.and(*predicates.toTypedArray())
        }
    }

    // -------------------------------------------------------------------
    // SESSION SEARCH SPECIFICATION
    // -------------------------------------------------------------------
    fun buildSessionSearchSpec(request: SessionSearchRequest, userId: Int): Specification<Session> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            val isPublic = cb.isTrue(root.get("isPublic"))
            val isPremade = cb.isTrue(root.get("isPremade"))
            val publicOrPremade = cb.or(isPublic, isPremade)

            // >> DB-Level Private Access Check <<
            val privateAccess = if (userId != 0 && query != null) {
                val userSq = query.subquery(Int::class.java)
                val userSqRoot = userSq.from(UserSessionAccess::class.java)
                userSq.select(cb.literal(1)).where(
                    cb.equal(userSqRoot.get<Session>("session"), root),
                    cb.equal(userSqRoot.join<Any, Any>("user").get<Int>("id"), userId)
                )

                val groupSq = query.subquery(Int::class.java)
                val groupSqRoot = groupSq.from(GroupSessionAccess::class.java)
                groupSq.select(cb.literal(1)).where(
                    cb.equal(groupSqRoot.get<Session>("session"), root),
                    cb.equal(groupSqRoot.join<Any, Any>("group").join<Any, Any>("members").get<Int>("id"), userId)
                )

                cb.or(cb.exists(userSq), cb.exists(groupSq))
            } else {
                cb.disjunction()
            }

            when (request.searchScope) {
                SearchScope.MY_ITEMS ->
                    predicates.add(cb.equal(root.get<User>("owner").get<Int>("id"), userId))

                SearchScope.PREMADE ->
                    predicates.add(isPremade)

                SearchScope.PUBLIC_COMMUNITY -> {
                    predicates.add(isPublic)
                    predicates.add(cb.isFalse(isPremade))
                    if (userId != 0) predicates.add(cb.notEqual(root.get<User>("owner").get<Int>("id"), userId))
                }

                SearchScope.SHARED_WITH_ME -> {
                    predicates.add(privateAccess)
                    if (userId != 0) predicates.add(cb.notEqual(root.get<User>("owner").get<Int>("id"), userId))
                    predicates.add(cb.isFalse(isPremade))
                    predicates.add(cb.isFalse(isPublic))
                }

                SearchScope.ALL_ACCESSIBLE -> {
                    val isOwner = cb.equal(root.get<User>("owner").get<Int>("id"), userId)
                    predicates.add(cb.or(privateAccess, publicOrPremade, isOwner))
                }
            }

            if (request.onlyFavorites && userId != 0) {
                predicates.add(cb.equal(root.join<Session, User>("favoritedByUsers").get<Int>("id"), userId))
            }

            request.searchTerm?.takeIf { it.isNotBlank() }?.let { term ->
                val p = "%${term.lowercase()}%"
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), p), cb.like(cb.lower(root.get("description")), p)))
            }

            request.phaseOfPlay?.let { predicates.add(cb.equal(root.get<Any>("phaseOfPlay"), it)) }
            request.ballContext?.let { predicates.add(cb.equal(root.get<Any>("ballContext"), it)) }
            request.drillFormat?.let { predicates.add(cb.equal(root.get<Any>("drillFormat"), it)) }
            request.targetAgeLevel?.let { predicates.add(cb.equal(root.get<String>("targetAgeLevel"), it)) }

            if (!request.tacticalActions.isNullOrEmpty()) {
                predicates.add(root.join<Session, Any>("tacticalActions", JoinType.LEFT).`in`(request.tacticalActions))
            }
            if (!request.qualityMakers.isNullOrEmpty()) {
                predicates.add(root.join<Session, Any>("qualityMakers", JoinType.LEFT).`in`(request.qualityMakers))
            }

            query?.distinct(true)
            cb.and(*predicates.toTypedArray())
        }
    }

    // -------------------------------------------------------------------
    // GAME TACTIC SEARCH SPECIFICATION
    // -------------------------------------------------------------------
    fun buildGameTacticSearchSpec(request: GameTacticSearchRequest, userId: Int): Specification<GameTactic> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            val isPublic = cb.isTrue(root.get("isPublic"))
            val isPremade = cb.isTrue(root.get("isPremade"))
            val publicOrPremade = cb.or(isPublic, isPremade)

            // >> DB-Level Private Access Check <<
            val privateAccess = if (userId != 0 && query != null) {
                val userSq = query.subquery(Int::class.java)
                val userSqRoot = userSq.from(UserGameTacticAccess::class.java)
                userSq.select(cb.literal(1)).where(
                    cb.equal(userSqRoot.get<GameTactic>("gameTactic"), root),
                    cb.equal(userSqRoot.join<Any, Any>("user").get<Int>("id"), userId)
                )

                val groupSq = query.subquery(Int::class.java)
                val groupSqRoot = groupSq.from(GroupGameTacticAccess::class.java)
                groupSq.select(cb.literal(1)).where(
                    cb.equal(groupSqRoot.get<GameTactic>("gameTactic"), root),
                    cb.equal(groupSqRoot.join<Any, Any>("group").join<Any, Any>("members").get<Int>("id"), userId)
                )

                cb.or(cb.exists(userSq), cb.exists(groupSq))
            } else {
                cb.disjunction()
            }

            when (request.searchScope) {
                SearchScope.MY_ITEMS ->
                    predicates.add(cb.equal(root.get<User>("owner").get<Int>("id"), userId))

                SearchScope.PREMADE ->
                    predicates.add(isPremade)

                SearchScope.PUBLIC_COMMUNITY -> {
                    predicates.add(isPublic)
                    predicates.add(cb.isFalse(isPremade))
                    if (userId != 0) predicates.add(cb.notEqual(root.get<User>("owner").get<Int>("id"), userId))
                }

                SearchScope.SHARED_WITH_ME -> {
                    predicates.add(privateAccess)
                    if (userId != 0) predicates.add(cb.notEqual(root.get<User>("owner").get<Int>("id"), userId))
                    predicates.add(cb.isFalse(isPremade))
                    predicates.add(cb.isFalse(isPublic))
                }

                SearchScope.ALL_ACCESSIBLE -> {
                    val isOwner = cb.equal(root.get<User>("owner").get<Int>("id"), userId)
                    predicates.add(cb.or(privateAccess, publicOrPremade, isOwner))
                }
            }

            if (request.onlyFavorites && userId != 0) {
                predicates.add(cb.equal(root.join<GameTactic, User>("favoritedByUsers").get<Int>("id"), userId))
            }

            request.searchTerm?.takeIf { it.isNotBlank() }?.let { term ->
                val p = "%${term.lowercase()}%"
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), p), cb.like(cb.lower(root.get("description")), p)))
            }

            query?.distinct(true)
            cb.and(*predicates.toTypedArray())
        }
    }
}