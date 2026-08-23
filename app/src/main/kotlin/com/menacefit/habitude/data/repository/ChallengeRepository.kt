package com.menacefit.habitude.data.repository

import com.menacefit.habitude.data.local.dao.ChallengeDao
import com.menacefit.habitude.data.mapper.toDomain
import com.menacefit.habitude.data.mapper.toEntity
import com.menacefit.habitude.domain.model.Challenge
import com.menacefit.habitude.domain.model.ChallengeStatus
import com.menacefit.habitude.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface ChallengeRepository {
    fun observeAll(): Flow<List<Challenge>>
    suspend fun getAll(): List<Challenge>
    suspend fun createChallenge(
        title: String,
        description: String,
        icon: String,
        startDate: LocalDate,
        durationDays: Int,
        linkedHabitId: String?,
        xpReward: Int,
    ): Challenge

    /** Toggles [date] as checked off; recomputes status (ACTIVE/COMPLETED/FAILED) against [today]. Returns null if the challenge doesn't exist. */
    suspend fun toggleCheckIn(challengeId: String, date: LocalDate, today: LocalDate): Challenge?
    suspend fun deleteChallenge(id: String)
}

class ChallengeRepositoryImpl(private val challengeDao: ChallengeDao) : ChallengeRepository {

    override fun observeAll(): Flow<List<Challenge>> = challengeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Challenge> = challengeDao.getAllOnce().map { it.toDomain() }

    override suspend fun createChallenge(
        title: String,
        description: String,
        icon: String,
        startDate: LocalDate,
        durationDays: Int,
        linkedHabitId: String?,
        xpReward: Int,
    ): Challenge {
        val challenge = Challenge(
            id = newId(), title = title, description = description, icon = icon, startDate = startDate,
            durationDays = durationDays, linkedHabitId = linkedHabitId, xpReward = xpReward,
            status = ChallengeStatus.ACTIVE, completedDates = emptySet(),
        )
        challengeDao.upsert(challenge.toEntity())
        return challenge
    }

    override suspend fun toggleCheckIn(challengeId: String, date: LocalDate, today: LocalDate): Challenge? {
        val existing = challengeDao.get(challengeId)?.toDomain() ?: return null
        val newDates = if (date in existing.completedDates) existing.completedDates - date else existing.completedDates + date
        val status = statusFor(existing.copy(completedDates = newDates), today)
        val updated = existing.copy(completedDates = newDates, status = status)
        challengeDao.update(updated.toEntity())
        return updated
    }

    override suspend fun deleteChallenge(id: String) {
        challengeDao.delete(id)
    }

    private fun statusFor(challenge: Challenge, today: LocalDate): ChallengeStatus = when {
        challenge.completedDates.size >= challenge.durationDays -> ChallengeStatus.COMPLETED
        today.isAfter(challenge.endDateExclusive) -> ChallengeStatus.FAILED
        else -> ChallengeStatus.ACTIVE
    }
}
