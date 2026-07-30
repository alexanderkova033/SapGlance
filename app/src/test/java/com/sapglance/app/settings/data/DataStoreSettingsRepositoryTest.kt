package com.sapglance.app.settings.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.common.truth.Truth.assertThat
import com.sapglance.core.settings.model.AppSettings
import com.sapglance.core.settings.model.VarietyLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DataStoreSettingsRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var repository: DataStoreSettingsRepository

    @BeforeEach
    fun setUp() {
        val dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = { File(tempDir, "settings.preferences_pb") },
            )
        repository = DataStoreSettingsRepository(dataStore)
    }

    @Test
    fun `defaults are returned when nothing has been written`() =
        runTest {
            assertThat(repository.settings.first()).isEqualTo(AppSettings.DEFAULT)
        }

    @Test
    fun `setVarietyLevel persists and is reflected in settings flow`() =
        runTest {
            repository.setVarietyLevel(VarietyLevel.PLAYFUL)
            assertThat(repository.settings.first().varietyLevel).isEqualTo(VarietyLevel.PLAYFUL)
        }

    @Test
    fun `falls back to the legacy boolean preference when the new key hasn't been written`() =
        runTest {
            val legacyDataStore =
                PreferenceDataStoreFactory.create(
                    produceFile = { File(tempDir, "legacy.preferences_pb") },
                )
            val legacyKey = booleanPreferencesKey("more_variety_enabled")
            legacyDataStore.edit { it[legacyKey] = true }
            val legacyRepository = DataStoreSettingsRepository(legacyDataStore)

            assertThat(legacyRepository.settings.first().varietyLevel).isEqualTo(VarietyLevel.PLAYFUL)
        }
}
