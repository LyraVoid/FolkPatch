package me.bmax.apatch.util

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import me.bmax.apatch.APApplication

/**
 * Reactive SharedPreferences reading utilities for Compose.
 * Automatically updates when the preference value changes externally.
 */

@Composable
fun rememberBoolPref(
    key: String,
    defaultValue: Boolean = false
): State<Boolean> {
    val prefs = APApplication.sharedPreferences
    val state = remember { mutableStateOf(prefs.getBoolean(key, defaultValue)) }

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                state.value = prefs.getBoolean(key, defaultValue)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return state
}

@Composable
fun rememberStringPref(
    key: String,
    defaultValue: String = ""
): State<String> {
    val prefs = APApplication.sharedPreferences
    val state = remember { mutableStateOf(prefs.getString(key, defaultValue) ?: defaultValue) }

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                state.value = prefs.getString(key, defaultValue) ?: defaultValue
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return state
}

@Composable
fun rememberFloatPref(
    key: String,
    defaultValue: Float = 0f
): State<Float> {
    val prefs = APApplication.sharedPreferences
    val state = remember { mutableStateOf(prefs.getFloat(key, defaultValue)) }

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                state.value = prefs.getFloat(key, defaultValue)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return state
}

@Composable
fun rememberIntPref(
    key: String,
    defaultValue: Int = 0
): State<Int> {
    val prefs = APApplication.sharedPreferences
    val state = remember { mutableStateOf(prefs.getInt(key, defaultValue)) }

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                state.value = prefs.getInt(key, defaultValue)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return state
}
