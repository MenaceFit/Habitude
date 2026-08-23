package com.menacefit.habitude.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Several domain models (suggested habits, quest descriptions, achievement
 * titles) carry a string-resource key rather than literal text, so business
 * logic never bakes in one language. This resolves such a key to the
 * actual localized string by looking up a matching entry in strings.xml —
 * the same mechanism for every "Key" field in the app, instead of a
 * hand-maintained when-block per feature that inevitably drifts out of
 * sync as keys are added.
 */
fun Context.resolveStringByKey(key: String, vararg formatArgs: Any): String {
    val resId = resources.getIdentifier(key, "string", packageName)
    if (resId == 0) return key
    return if (formatArgs.isEmpty()) getString(resId) else getString(resId, *formatArgs)
}

@Composable
fun stringResourceByKey(key: String, vararg formatArgs: Any): String {
    val context = LocalContext.current
    return remember(key, formatArgs) { context.resolveStringByKey(key, *formatArgs) }
}
