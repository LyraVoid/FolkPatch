package me.bmax.apatch.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutVariantTest {
    @Test
    fun unknownValuesUseM3E() {
        assertEquals(LayoutVariant.M3E, LayoutVariant.fromValue(null))
        assertEquals(LayoutVariant.M3E, LayoutVariant.fromValue("unknown"))
    }

    @Test
    fun traditionalValueIsPreserved() {
        assertEquals(LayoutVariant.TRADITIONAL, LayoutVariant.fromValue(LayoutVariant.TRADITIONAL))
    }
}
