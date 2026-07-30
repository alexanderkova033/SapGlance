package com.sapglance.app.tips.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.sapglance.core.tips.port.TipHistoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreTipHistoryRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var repository: DataStoreTipHistoryRepository

    @BeforeEach
    fun setUp() {
        val dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = { File(tempDir, "tip_history.preferences_pb") },
            )
        repository = DataStoreTipHistoryRepository(dataStore)
    }

    @Test
    fun `recentTips is empty before anything is stored`() =
        runTest {
            assertThat(repository.recentTips.first()).isEmpty()
        }

    @Test
    fun `recordTip appends and preserves order in the flow`() =
        runTest {
            repository.recordTip("Take a deep breath.")
            repository.recordTip("Stand up and stretch.")

            assertThat(repository.recentTips.first())
                .containsExactly("Take a deep breath.", "Stand up and stretch.")
                .inOrder()
        }

    @Test
    fun `recordTip trims to the MAX_RECENT_TIPS most recent entries`() =
        runTest {
            val total = TipHistoryRepository.MAX_RECENT_TIPS + 5
            repeat(total) { index -> repository.recordTip("Tip $index") }

            val recentTips = repository.recentTips.first()

            assertThat(recentTips).hasSize(TipHistoryRepository.MAX_RECENT_TIPS)
            // Oldest entries ("Tip 0".."Tip 4") should have rolled off, newest kept.
            assertThat(recentTips.first()).isEqualTo("Tip 5")
            assertThat(recentTips.last()).isEqualTo("Tip ${total - 1}")
        }
}
