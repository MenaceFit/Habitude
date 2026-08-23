package com.menacefit.habitude.data.local

import androidx.room.TypeConverter
import com.menacefit.habitude.domain.model.AchievementType
import com.menacefit.habitude.domain.model.ChallengeStatus
import com.menacefit.habitude.domain.model.DayPerformance
import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType
import com.menacefit.habitude.domain.model.MoodLevel
import com.menacefit.habitude.domain.model.QuestMetric
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/**
 * All Room type conversions in one place. Room applies a non-null converter
 * to nullable columns automatically (skipping the call for a null value), so
 * every function here is written against the non-null type only.
 *
 * Polymorphic domain types ([Frequency], [HabitTarget]) are persisted as
 * their own `@Serializable` JSON representation rather than a hand-rolled
 * encoding — one less thing that can drift out of sync with the domain
 * model.
 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromInstant(value: Instant): String = value.toString()

    @TypeConverter
    fun toInstant(value: String): Instant = Instant.parse(value)

    @TypeConverter
    fun fromStringSet(value: Set<String>): String = json.encodeToString(SetSerializer(String.serializer()), value)

    @TypeConverter
    fun toStringSet(value: String): Set<String> = json.decodeFromString(SetSerializer(String.serializer()), value)

    @TypeConverter
    fun fromLocalDateSet(value: Set<LocalDate>): String =
        json.encodeToString(SetSerializer(String.serializer()), value.map { it.toString() }.toSet())

    @TypeConverter
    fun toLocalDateSet(value: String): Set<LocalDate> =
        json.decodeFromString(SetSerializer(String.serializer()), value).map { LocalDate.parse(it) }.toSet()

    @TypeConverter
    fun fromFrequency(value: Frequency): String = json.encodeToString(Frequency.serializer(), value)

    @TypeConverter
    fun toFrequency(value: String): Frequency = json.decodeFromString(Frequency.serializer(), value)

    @TypeConverter
    fun fromHabitTarget(value: HabitTarget): String = json.encodeToString(HabitTarget.serializer(), value)

    @TypeConverter
    fun toHabitTarget(value: String): HabitTarget = json.decodeFromString(HabitTarget.serializer(), value)

    @TypeConverter
    fun fromHabitCategory(value: HabitCategory): String = value.name

    @TypeConverter
    fun toHabitCategory(value: String): HabitCategory = enumValueOf(value)

    @TypeConverter
    fun fromHabitType(value: HabitType): String = value.name

    @TypeConverter
    fun toHabitType(value: String): HabitType = enumValueOf(value)

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String = value.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty = enumValueOf(value)

    @TypeConverter
    fun fromDayPerformance(value: DayPerformance): String = value.name

    @TypeConverter
    fun toDayPerformance(value: String): DayPerformance = enumValueOf(value)

    @TypeConverter
    fun fromAchievementType(value: AchievementType): String = value.name

    @TypeConverter
    fun toAchievementType(value: String): AchievementType = enumValueOf(value)

    @TypeConverter
    fun fromMoodLevel(value: MoodLevel): String = value.name

    @TypeConverter
    fun toMoodLevel(value: String): MoodLevel = enumValueOf(value)

    @TypeConverter
    fun fromQuestMetric(value: QuestMetric): String = value.name

    @TypeConverter
    fun toQuestMetric(value: String): QuestMetric = enumValueOf(value)

    @TypeConverter
    fun fromChallengeStatus(value: ChallengeStatus): String = value.name

    @TypeConverter
    fun toChallengeStatus(value: String): ChallengeStatus = enumValueOf(value)

    @TypeConverter
    fun fromHabitCategorySet(value: Set<HabitCategory>): String =
        json.encodeToString(SetSerializer(String.serializer()), value.map { it.name }.toSet())

    @TypeConverter
    fun toHabitCategorySet(value: String): Set<HabitCategory> =
        json.decodeFromString(SetSerializer(String.serializer()), value).map { enumValueOf<HabitCategory>(it) }.toSet()
}
