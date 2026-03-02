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
    fun buildPracticeSearchSpec(request: PracticeSearchRequest, accessibleIds: Set<Int>, userId: Int): Specification<Practice> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            val isPublic = cb.isTrue(root.get("isPublic"))
            val isPremade = cb.isTrue(root.get("isPremade"))
            val publicOrPremade = cb.or(isPublic, isPremade)

            val privateAccess = if (userId != 0 && accessibleIds.isNotEmpty()) {
                root.get<Int>("id").`in`(accessibleIds)
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
                    predicates.add(cb.or(privateAccess, publicOrPremade))
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
    fun buildSessionSearchSpec(request: SessionSearchRequest, accessibleIds: Set<Int>, userId: Int): Specification<Session> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            val isPublic = cb.isTrue(root.get("isPublic"))
            val isPremade = cb.isTrue(root.get("isPremade"))
            val publicOrPremade = cb.or(isPublic, isPremade)

            val privateAccess = if (userId != 0 && accessibleIds.isNotEmpty()) {
                root.get<Int>("id").`in`(accessibleIds)
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
                    predicates.add(cb.or(privateAccess, publicOrPremade))
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
    fun buildGameTacticSearchSpec(request: GameTacticSearchRequest, accessibleIds: Set<Int>, userId: Int): Specification<GameTactic> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            val isPublic = cb.isTrue(root.get("isPublic"))
            val isPremade = cb.isTrue(root.get("isPremade"))
            val publicOrPremade = cb.or(isPublic, isPremade)

            val privateAccess = if (userId != 0 && accessibleIds.isNotEmpty()) {
                root.get<Int>("id").`in`(accessibleIds)
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
                    predicates.add(cb.or(privateAccess, publicOrPremade))
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