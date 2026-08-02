package com.sapglance.app.settings.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.sapglance.core.settings.AppSettings
import com.sapglance.core.settings.PoolAmount
import com.sapglance.core.settings.PoolMix
import com.sapglance.core.settings.TipLanguage
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
    fun `setPoolMix persists every pool, not just the one that changed`() =
        runTest {
            val mix =
                PoolMix(
                    practical = PoolAmount.NONE,
                    philosophy = PoolAmount.PLENTY,
                    motivation = PoolAmount.NONE,
                    wellbeing = PoolAmount.SOME,
                )

            repository.setPoolMix(mix)

            assertThat(repository.settings.first().poolMix).isEqualTo(mix)
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
            val mix = PoolMix.DEFAULT.copy(practical = PoolAmount.SOME)
            repository.setLanguage(TipLanguage.RUSSIAN)
            repository.setPoolMix(mix)

            val settings = repository.settings.first()
            assertThat(settings.language).isEqualTo(TipLanguage.RUSSIAN)
            assertThat(settings.poolMix).isEqualTo(mix)
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

    /**
     * Two upgrades' worth of migration, and the oldest one still has to work: someone who set the
     * boolean toggle, never opened Settings again through the `VarietyLevel` release, and is now
     * on the per-pool one.
     */
    @Test
    fun `falls back to the legacy boolean preference when no newer key has been written`() =
        runTest {
            val legacyDataStore =
                PreferenceDataStoreFactory.create(
                    produceFile = { File(tempDir, "legacy.preferences_pb") },
                )
            val legacyKey = booleanPreferencesKey("more_variety_enabled")
            legacyDataStore.edit { it[legacyKey] = true }
            val legacyRepository = DataStoreSettingsRepository(legacyDataStore)

            assertThat(legacyRepository.settings.first().poolMix)
                .isEqualTo(PoolMix.DEFAULT.copy(practical = PoolAmount.SOME))
        }

    /**
     * The migration that matters most, because it is the one an existing reader will actually hit.
     * PLAYFUL meant 80% tone and the new ladder's nearest step is 100%, but migrating to
     * [PoolAmount.NONE] would mean an upgrade silently switching a pool off. Less tone than they
     * chose is a change they can see and undo; a pool vanishing is not.
     */
    @Test
    fun `a stored PLAYFUL migrates to fewer practical tips, never to none`() =
        runTest {
            val oldDataStore =
                PreferenceDataStoreFactory.create(
                    produceFile = { File(tempDir, "variety-level.preferences_pb") },
                )
            oldDataStore.edit { it[stringPreferencesKey("variety_level")] = "PLAYFUL" }

            val mix = DataStoreSettingsRepository(oldDataStore).settings.first().poolMix

            assertThat(mix.practical).isEqualTo(PoolAmount.SOME)
            assertThat(mix.isSilent).isFalse()
        }

    @Test
    fun `a stored PRACTICAL migrates to the default, which is what it always meant`() =
        runTest {
            val oldDataStore =
                PreferenceDataStoreFactory.create(
                    produceFile = { File(tempDir, "variety-practical.preferences_pb") },
                )
            oldDataStore.edit { it[stringPreferencesKey("variety_level")] = "PRACTICAL" }

            assertThat(DataStoreSettingsRepository(oldDataStore).settings.first().poolMix)
                .isEqualTo(PoolMix.DEFAULT)
        }

    /** Same reasoning as the language case: what is on disk was written by a version that may not
     * be this one, and an unknown amount must read as "not set" rather than crash the widget. */
    @Test
    fun `an unrecognised stored amount falls back to the default instead of throwing`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory.create(
                    produceFile = { File(tempDir, "unknown-amount.preferences_pb") },
                )
            dataStore.edit { it[stringPreferencesKey("pool_philosophy")] = "LOADS" }

            val mix = DataStoreSettingsRepository(dataStore).settings.first().poolMix

            assertThat(mix.philosophy).isEqualTo(PoolMix.DEFAULT.philosophy)
        }
}
