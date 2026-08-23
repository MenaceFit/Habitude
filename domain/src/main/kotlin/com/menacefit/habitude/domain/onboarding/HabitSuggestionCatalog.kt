package com.menacefit.habitude.domain.onboarding

import com.menacefit.habitude.domain.model.Difficulty
import com.menacefit.habitude.domain.model.Frequency
import com.menacefit.habitude.domain.model.HabitCategory
import com.menacefit.habitude.domain.model.HabitTarget
import com.menacefit.habitude.domain.model.HabitType

/**
 * A ready-to-create habit suggested during onboarding. [nameKey]/[descriptionKey]
 * are string-resource keys (not literal text) so suggestions are fully
 * localized; the UI resolves them and lets the user rename/tweak before
 * adding, per spec section 5 ("ajouter / supprimer / modifier / ignorer").
 */
data class SuggestedHabitTemplate(
    val id: String,
    val nameKey: String,
    val icon: String,
    val category: HabitCategory,
    val type: HabitType,
    val target: HabitTarget,
    val difficulty: Difficulty,
    val frequency: Frequency = Frequency.Daily,
)

object HabitSuggestionCatalog {

    fun suggestionsFor(category: HabitCategory): List<SuggestedHabitTemplate> = catalog[category].orEmpty()

    private val catalog: Map<HabitCategory, List<SuggestedHabitTemplate>> = mapOf(
        HabitCategory.SPORT to listOf(
            template("sport_workout", "suggestion_sport_workout", "🏋️", HabitCategory.SPORT, HabitType.DURATION, HabitTarget.DurationTarget(30), Difficulty.MEDIUM),
            template("sport_steps", "suggestion_sport_steps", "🚶", HabitCategory.SPORT, HabitType.COUNT, HabitTarget.CountTarget(8000, "steps"), Difficulty.EASY),
            template("sport_stretch", "suggestion_sport_stretch", "🤸", HabitCategory.SPORT, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
            template("sport_pushups", "suggestion_sport_pushups", "💪", HabitCategory.SPORT, HabitType.COUNT, HabitTarget.CountTarget(50, "reps"), Difficulty.HARD),
        ),
        HabitCategory.DISCIPLINE to listOf(
            template("discipline_wake_up", "suggestion_discipline_wake_up", "⏰", HabitCategory.DISCIPLINE, HabitType.TIME, HabitTarget.TimeTarget(7, 0, isBefore = true), Difficulty.MEDIUM),
            template("discipline_make_bed", "suggestion_discipline_make_bed", "🛏️", HabitCategory.DISCIPLINE, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
            template("discipline_read", "suggestion_discipline_read", "📖", HabitCategory.DISCIPLINE, HabitType.DURATION, HabitTarget.DurationTarget(20), Difficulty.EASY),
            template("discipline_no_phone", "suggestion_discipline_no_phone", "📵", HabitCategory.DISCIPLINE, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.MEDIUM),
            template("discipline_water", "suggestion_discipline_water", "💧", HabitCategory.DISCIPLINE, HabitType.QUANTITY, HabitTarget.QuantityTarget(2.0, "L"), Difficulty.EASY),
        ),
        HabitCategory.STUDY to listOf(
            template("study_review", "suggestion_study_review", "📚", HabitCategory.STUDY, HabitType.DURATION, HabitTarget.DurationTarget(45), Difficulty.MEDIUM),
            template("study_flashcards", "suggestion_study_flashcards", "🗂️", HabitCategory.STUDY, HabitType.COUNT, HabitTarget.CountTarget(20, "cards"), Difficulty.EASY),
            template("study_no_distraction", "suggestion_study_no_distraction", "🎯", HabitCategory.STUDY, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.MEDIUM),
        ),
        HabitCategory.WORK to listOf(
            template("work_deep_focus", "suggestion_work_deep_focus", "💻", HabitCategory.WORK, HabitType.DURATION, HabitTarget.DurationTarget(90), Difficulty.HARD),
            template("work_inbox_zero", "suggestion_work_inbox_zero", "📧", HabitCategory.WORK, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
            template("work_plan_day", "suggestion_work_plan_day", "🗒️", HabitCategory.WORK, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
        ),
        HabitCategory.FINANCE to listOf(
            template("finance_track_spending", "suggestion_finance_track_spending", "💰", HabitCategory.FINANCE, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
            template("finance_no_impulse", "suggestion_finance_no_impulse", "🛍️", HabitCategory.FINANCE, HabitType.LIMIT, HabitTarget.LimitTarget(0.0, "achats impulsifs"), Difficulty.MEDIUM),
            template("finance_save", "suggestion_finance_save", "🏦", HabitCategory.FINANCE, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.MEDIUM, Frequency.TimesPerMonth(1)),
        ),
        HabitCategory.SLEEP to listOf(
            template("sleep_bedtime", "suggestion_sleep_bedtime", "😴", HabitCategory.SLEEP, HabitType.TIME, HabitTarget.TimeTarget(23, 0, isBefore = true), Difficulty.MEDIUM),
            template("sleep_no_screen", "suggestion_sleep_no_screen", "📴", HabitCategory.SLEEP, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.MEDIUM),
            template("sleep_8h", "suggestion_sleep_8h", "🌙", HabitCategory.SLEEP, HabitType.DURATION, HabitTarget.DurationTarget(480), Difficulty.EASY),
        ),
        HabitCategory.NUTRITION to listOf(
            template("nutrition_veggies", "suggestion_nutrition_veggies", "🥗", HabitCategory.NUTRITION, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
            template("nutrition_no_sugar", "suggestion_nutrition_no_sugar", "🍬", HabitCategory.NUTRITION, HabitType.LIMIT, HabitTarget.LimitTarget(0.0, "sucreries"), Difficulty.MEDIUM),
            template("nutrition_breakfast", "suggestion_nutrition_breakfast", "🍳", HabitCategory.NUTRITION, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
        ),
        HabitCategory.HEALTH to listOf(
            template("health_water", "suggestion_health_water", "💧", HabitCategory.HEALTH, HabitType.QUANTITY, HabitTarget.QuantityTarget(2.0, "L"), Difficulty.EASY),
            template("health_vitamins", "suggestion_health_vitamins", "💊", HabitCategory.HEALTH, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
            template("health_meditate", "suggestion_health_meditate", "🧘", HabitCategory.HEALTH, HabitType.DURATION, HabitTarget.DurationTarget(10), Difficulty.MEDIUM),
        ),
        HabitCategory.SCREEN_TIME to listOf(
            template("screen_social_media", "suggestion_screen_social_media", "📱", HabitCategory.SCREEN_TIME, HabitType.LIMIT, HabitTarget.LimitTarget(120.0, "min"), Difficulty.MEDIUM),
            template("screen_no_phone_morning", "suggestion_screen_no_phone_morning", "🌅", HabitCategory.SCREEN_TIME, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.MEDIUM),
            template("screen_no_bed", "suggestion_screen_no_bed", "🛌", HabitCategory.SCREEN_TIME, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
        ),
        HabitCategory.WELLBEING to listOf(
            template("wellbeing_meditate", "suggestion_wellbeing_meditate", "🧘", HabitCategory.WELLBEING, HabitType.DURATION, HabitTarget.DurationTarget(15), Difficulty.MEDIUM),
            template("wellbeing_gratitude", "suggestion_wellbeing_gratitude", "🙏", HabitCategory.WELLBEING, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
            template("wellbeing_outside", "suggestion_wellbeing_outside", "🌳", HabitCategory.WELLBEING, HabitType.DURATION, HabitTarget.DurationTarget(20), Difficulty.EASY),
        ),
        HabitCategory.PRODUCTIVITY to listOf(
            template("productivity_top3", "suggestion_productivity_top3", "🎯", HabitCategory.PRODUCTIVITY, HabitType.CHECKLIST,
                HabitTarget.ChecklistTarget(emptyList()), Difficulty.MEDIUM),
            template("productivity_no_multitask", "suggestion_productivity_no_multitask", "🧩", HabitCategory.PRODUCTIVITY, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.MEDIUM),
            template("productivity_review_day", "suggestion_productivity_review_day", "📝", HabitCategory.PRODUCTIVITY, HabitType.BOOLEAN, HabitTarget.BooleanTarget, Difficulty.EASY),
        ),
        HabitCategory.OTHER to emptyList(),
    )

    private fun template(
        id: String,
        nameKey: String,
        icon: String,
        category: HabitCategory,
        type: HabitType,
        target: HabitTarget,
        difficulty: Difficulty,
        frequency: Frequency = Frequency.Daily,
    ) = SuggestedHabitTemplate(id, nameKey, icon, category, type, target, difficulty, frequency)
}
