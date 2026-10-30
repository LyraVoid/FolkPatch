package me.bmax.apatch.ui.screen.module

import java.text.Collator

enum class ModuleSortGroup(val value: String) {
    MetaModule("metamodule"),
    Zygisk("zygisk"),
    LSPosed("lsposed"),
    WebUi("webui"),
    ActionScript("action"),
}

val ModuleSortPriorityGroups = listOf(
    ModuleSortGroup.MetaModule,
    ModuleSortGroup.Zygisk,
    ModuleSortGroup.LSPosed,
    ModuleSortGroup.WebUi,
    ModuleSortGroup.ActionScript,
)

private val ZygiskProviderIds = setOf(
    "zygisksu",
    "zygisknext",
    "rezygisk",
    "neozygisk",
    "shirokozygisk",
    "onyxzygisk",
    "yukizygisk",
    "brezygisk",
)

data class ModuleSortFacts(
    val id: String,
    val name: String,
    val metaModule: Boolean,
    val hasWebUi: Boolean,
    val hasActionScript: Boolean,
    val enabled: Boolean,
)

fun ModuleSortFacts.sortGroup(): ModuleSortGroup? = when {
    metaModule -> ModuleSortGroup.MetaModule
    id.contains("zygisk", ignoreCase = true) -> ModuleSortGroup.Zygisk
    name.contains("LSPosed", ignoreCase = true) -> ModuleSortGroup.LSPosed
    hasWebUi -> ModuleSortGroup.WebUi
    hasActionScript -> ModuleSortGroup.ActionScript
    else -> null
}

fun moduleSortComparator(
    collator: Collator,
    priorities: Set<ModuleSortGroup>,
    enabledFirst: Boolean,
): Comparator<ModuleSortFacts> {
    val ranking = ModuleSortPriorityGroups.filter { it in priorities }
    return compareBy<ModuleSortFacts> { facts ->
        facts.sortGroup()?.let(ranking::indexOf)?.takeIf { it >= 0 } ?: ranking.size
    }.thenBy { facts ->
        if (facts.sortGroup() == ModuleSortGroup.Zygisk &&
            facts.id.lowercase() !in ZygiskProviderIds
        ) {
            1
        } else {
            0
        }
    }.thenBy { facts -> enabledFirst && !facts.enabled }
        .thenBy(collator) { it.id }
}

fun customModuleOrderComparator(
    collator: Collator,
    order: List<String>,
): Comparator<ModuleSortFacts> {
    val positions = order.withIndex().associate { it.value to it.index }
    return compareBy<ModuleSortFacts> { positions[it.id] ?: Int.MAX_VALUE }
        .thenBy(collator) { it.id }
}

fun reconcileModuleCustomOrder(order: List<String>, moduleIds: List<String>): List<String> {
    if (order.isEmpty() || moduleIds.isEmpty()) return order
    val installed = moduleIds.toHashSet()
    val kept = order.filter { it in installed }
    val keptIds = kept.toHashSet()
    return kept + moduleIds.filter { it !in keptIds }
}

object ModuleSortPriorityStore {
    const val Key = "module_sort_groups"

    val Default = ModuleSortPriorityGroups.toSet()

    fun encode(groups: Set<ModuleSortGroup>): String =
        ModuleSortPriorityGroups.filter { it in groups }.joinToString(",") { it.value }

    fun decode(stored: String?): Set<ModuleSortGroup> {
        if (stored == null) return Default
        val tokens = stored.split(',').map(String::trim).filter(String::isNotEmpty)
        val known = tokens.mapNotNullTo(mutableSetOf()) { token ->
            ModuleSortPriorityGroups.firstOrNull { it.value == token }
        }
        return if (known.isEmpty() && tokens.isNotEmpty()) Default else known
    }
}

object ModuleCustomOrderStore {
    const val Key = "module_sort_custom_order"

    fun encode(ids: List<String>): String =
        ids.filter(String::isNotBlank).distinct().joinToString("\n")

    fun decode(stored: String?): List<String> =
        stored?.split('\n')?.filter(String::isNotEmpty)?.distinct().orEmpty()
}
