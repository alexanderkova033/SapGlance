package com.sapglance.core.widget.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class WidgetStyleTest {
    @Test
    fun `the same tip text always maps to the same style`() {
        val tipText = "Stand up and stretch for a moment."
        assertThat(WidgetStyle.forTip(tipText)).isEqualTo(WidgetStyle.forTip(tipText))
    }

    @Test
    fun `different tip texts can map to different styles`() {
        val styles = (1..20).map { WidgetStyle.forTip("Tip number $it") }.toSet()
        assertThat(styles.size).isGreaterThan(1)
    }

    @Test
    fun `negative hash codes still resolve to a valid entry`() {
        // This text's hashCode() is negative (-356442182) — a naive `hashCode() % entries.size`
        // would produce a negative array index and crash; `Math.floorMod` must not.
        assertThat(WidgetStyle.entries).contains(WidgetStyle.forTip("Get up and walk around for two minutes."))
    }
}
