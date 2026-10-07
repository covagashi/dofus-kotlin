package org.starloco.locos.common

/**
 * Split with Java String.split semantics: trailing empty strings are dropped,
 * so "a|b|".splitJ("|") == ["a","b"] like Java's "a|b|".split("\\|").
 * Kotlin's split keeps trailing empties, which produced phantom elements that
 * broke parsing during the Java->Kotlin migration (e.g. Spell.parseEffect).
 * An empty input yields [""], matching Java.
 */
fun String.splitJ(delimiter: String): List<String> =
    if (isEmpty()) listOf("") else split(delimiter).dropLastWhile { it.isEmpty() }

fun String.splitJ(delimiter: Regex): List<String> =
    if (isEmpty()) listOf("") else split(delimiter).dropLastWhile { it.isEmpty() }
