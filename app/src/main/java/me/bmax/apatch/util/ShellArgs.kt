package me.bmax.apatch.util

fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
