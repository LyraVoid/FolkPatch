package me.bmax.apatch.ui

import androidx.compose.runtime.staticCompositionLocalOf

object LayoutVariant {
    const val TRADITIONAL = "default"
    const val M3E = "m3e"

    fun fromValue(value: String?): String = when (value) {
        TRADITIONAL -> TRADITIONAL
        else -> M3E
    }

    val supported: List<String> = listOf(M3E, TRADITIONAL)
}

val LocalLayoutVariant = staticCompositionLocalOf { LayoutVariant.M3E }
