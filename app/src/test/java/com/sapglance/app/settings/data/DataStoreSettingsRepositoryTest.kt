package com.sapglance.app.settings.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.sapglance.core.settings.AppSettings
import com.sapglance.core.settings.TipLanguage
import com.sapglance.core.settings.VarietyLevel
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
    fun `setLanguage persists and is reflected in settings flow`() =
        runTest {
            repository.setLanguage(TipLanguage.RUSSIAN)
            assertThat(repository.settings.first().language).isEqualTo(TipLanguage.RUSSIAN)
        }

    @Test
    fun `the two settings are independent`() =
        runTest {
            repository.setLanguage(TipLanguage.RUSSIAN)
            repository.setVarietyLevel(VarietyLevel.BALANCED)

            val settings = repository.settings.first()
            assertThat(settings.language).isEqualTo(TipLanguage.RUSSIAN)
            assertThat(settings.varietyLevel).isEqualTo(VarietyLevel.BALANCED)
        }

    /**
     * Storage outlives code: a language constant renamed or dropped in some later version leaves
     * its old name on disk. That must read as "not set" rather than throwing, because the widget
     * collects this flow, so an exception here would be a crash on every preference write rather
     * than a single error.
     */
    @Test
    fun `an unrecognised stored language falls back to the default instead of throwing`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory.create(
                    produceFile = { File(tempDir, "unknown-language.preferences_pb") },
                )
            dataStore.edit { it[stringPreferencesKey("tip_language")] = "KLINGON" }

            val settings = DataStoreSettingsRepository(dataStore).settings.first()

            assertThat(settings.language).isEqualTo(AppSettings.DEFAULT.language)
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
