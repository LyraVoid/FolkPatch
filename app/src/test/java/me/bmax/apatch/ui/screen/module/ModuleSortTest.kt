package me.bmax.apatch.ui.screen.module

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Collator
import java.util.Locale

class ModuleSortTest {
    private val collator = Collator.getInstance(Locale.ENGLISH)

    @Test
    fun priorityGroupsComeBeforeAlphabeticalModules() {
        val modules = listOf(
            fact("alpha"),
            fact("zygisknext"),
            fact("meta", meta = true),
        )

        val sorted = modules.sortedWith(
            moduleSortComparator(collator, ModuleSortPriorityGroups.toSet(), enabledFirst = false)
        )

        assertEquals(listOf("meta", "zygisknext", "alpha"), sorted.map { it.id })
    }

    @Test
    fun zygiskProviderPrecedesZygiskConsumers() {
        val modules = listOf(
            fact("foozygisk"),
            fact("zygisknext"),
        )

        val sorted = modules.sortedWith(
            moduleSortComparator(
                collator,
                setOf(ModuleSortGroup.Zygisk),
                enabledFirst = false,
            )
        )

        assertEquals(listOf("zygisknext", "foozygisk"), sorted.map { it.id })
    }

    @Test
    fun enabledFirstStaysInsideTheSameGroup() {
        val modules = listOf(
            fact("alpha", enabled = false),
            fact("meta-off", meta = true, enabled = false),
            fact("meta-on", meta = true),
        )

        val sorted = modules.sortedWith(
            moduleSortComparator(
                collator,
                setOf(ModuleSortGroup.MetaModule),
                enabledFirst = true,
            )
        )

        assertEquals(listOf("meta-on", "meta-off", "alpha"), sorted.map { it.id })
    }

    @Test
    fun customOrderKeepsUnknownModulesAtTheEnd() {
        val modules = listOf(
            fact("new"),
            fact("second"),
            fact("first"),
        )

        val sorted = modules.sortedWith(
            customModuleOrderComparator(collator, listOf("first", "second"))
        )

        assertEquals(listOf("first", "second", "new"), sorted.map { it.id })
        assertTrue(reconcileModuleCustomOrder(listOf("first"), listOf("first", "new")) == listOf("first", "new"))
    }

    private fun fact(
        id: String,
        meta: Boolean = false,
        enabled: Boolean = true,
    ) = ModuleSortFacts(
        id = id,
        name = id,
        metaModule = meta,
        hasWebUi = false,
        hasActionScript = false,
        enabled = enabled,
    )
}
