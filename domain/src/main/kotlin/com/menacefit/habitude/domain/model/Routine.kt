package com.menacefit.habitude.domain.model

import kotlinx.serialization.Serializable

/** A named, ordered group of existing habits (e.g. "Routine matin"). [habitIds] is ordered; progress is derived live from whether each habit is completed today, not tracked separately. */
@Serializable
data class Routine(
    val id: String,
    val name: String,
    val icon: String,
    val sortOrder: Int,
    val habitIds: List<String>,
)
