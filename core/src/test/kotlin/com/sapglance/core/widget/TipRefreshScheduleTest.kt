package com.sapglance.core.widget

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TipRefreshScheduleTest {
    @Test
    fun `does not advance before the tick threshold`() {
        for (ticks in 0 until TICKS_UNTIL_ADVANCE - 1) {
            assertThat(shouldAdvanceTip(ticks)).isFalse()
        }
    }

    @Test
    fun `advances exactly on the tick threshold`() {
        assertThat(shouldAdvanceTip(TICKS_UNTIL_ADVANCE - 1)).isTrue()
    }

    @Test
    fun `still advances past the threshold`() {
        assertThat(shouldAdvanceTip(TICKS_UNTIL_ADVANCE)).isTrue()
    }
}
