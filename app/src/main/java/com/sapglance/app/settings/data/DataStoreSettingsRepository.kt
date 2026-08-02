package com.sapglance.app.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sapglance.core.settings.AppSettings
import com.sapglance.core.settings.PoolAmount
import com.sapglance.core.settings.PoolMix
import com.sapglance.core.settings.SettingsRepository
import com.sapglance.core.settings.TipLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [SettingsRepository] backed by Jetpack DataStore. Depends on [DataStore] rather than
 * [android.content.Context] so it can be unit-tested on the plain JVM with an
 * in-memory-backed DataStore, with no Robolectric/Android dependency.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    override suspend fun setPoolMix(mix: PoolMix) {
        dataStore.edit {
            it[Keys.POOL_PRACTICAL] = mix.practical.name
            it[Keys.POOL_PHILOSOPHY] = mix.philosophy.name
            it[Keys.POOL_MOTIVATION] = mix.motivation.name
            it[Keys.POOL_WELLBEING] = mix.wellbeing.name
        }
    }

    override suspend fun setLanguage(language: TipLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    /**
     * An unreadable stored value degrades to the default rather than throwing, for every setting
     * and for the same reason: this parses whatever is on disk, and what is on disk was written
     * by a version of the app that may not be this one. An enum constant that has since been
     * renamed or dropped must read as "not set", not as a crash on every DataStore emission —
     * which, since the widget collects that flow, would be a crash loop rather than one error.
     */
    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            poolMix = poolMix(),
            language =
                TipLanguage.fromNameOrNull(this[Keys.LANGUAGE])
                    ?: AppSettings.DEFAULT.language,
        )

    /**
     * Reads the four per-pool amounts, falling back through two older shapes of this setting so
     * nobody's preference is silently reset on upgrade: first [Keys.LEGACY_VARIETY_LEVEL], the
     * single three-position control replaced on 2026-08-02, then
     * [Keys.LEGACY_MORE_VARIETY_ENABLED], the boolean toggle that one replaced.
     *
     * The amounts are read individually rather than all-or-nothing, so a partial write (the
     * process dying mid-`edit`, which DataStore's atomicity makes unlikely rather than
     * impossible) leaves the pools that did land in place instead of discarding all four.
     */
    private fun Preferences.poolMix(): PoolMix {
        val migrated = legacyPoolMix()
        return PoolMix(
            practical = amount(Keys.POOL_PRACTICAL) ?: migrated.practical,
            philosophy = amount(Keys.POOL_PHILOSOPHY) ?: migrated.philosophy,
            motivation = amount(Keys.POOL_MOTIVATION) ?: migrated.motivation,
            wellbeing = amount(Keys.POOL_WELLBEING) ?: migrated.wellbeing,
        )
    }

    private fun Preferences.amount(key: Preferences.Key<String>): PoolAmount? =
        this[key]?.let { runCatching { PoolAmount.valueOf(it) }.getOrNull() }

    /**
     * What the two older controls become.
     *
     * `PRACTICAL` and a `false` toggle are exactly [PoolMix.DEFAULT], which is why that default
     * was chosen to be behaviourally identical to them.
     *
     * `PLAYFUL` and a `true` toggle are the interesting case, and the mapping is deliberately not
     * the arithmetically closest one. PLAYFUL gave the tone pools 80% of a draw; the new ladder
     * offers 50% ([PoolAmount.SOME]) or 100% ([PoolAmount.NONE]), and 80 is nearer 100. Migrating
     * to NONE would mean an upgrade silently switching a pool off — taking something away that
     * the reader never asked to lose, in a release where "off" became possible for the first
     * time. So they land on SOME and get *less* tone than they chose, which is a change they can
     * see and undo in one tap, rather than a pool vanishing without explanation.
     */
    private fun Preferences.legacyPoolMix(): PoolMix {
        val moreVariety =
            when (this[Keys.LEGACY_VARIETY_LEVEL]) {
                "PLAYFUL", "BALANCED" -> true
                "PRACTICAL" -> false
                else -> this[Keys.LEGACY_MORE_VARIETY_ENABLED] ?: return PoolMix.DEFAULT
            }
        return if (moreVariety) PoolMix.DEFAULT.copy(practical = PoolAmount.SOME) else PoolMix.DEFAULT
    }

    private object Keys {
        val POOL_PRACTICAL = stringPreferencesKey("pool_practical")
        val POOL_PHILOSOPHY = stringPreferencesKey("pool_philosophy")
        val POOL_MOTIVATION = stringPreferencesKey("pool_motivation")
        val POOL_WELLBEING = stringPreferencesKey("pool_wellbeing")
        val LANGUAGE = stringPreferencesKey("tip_language")
        val LEGACY_VARIETY_LEVEL = stringPreferencesKey("variety_level")
        val LEGACY_MORE_VARIETY_ENABLED = booleanPreferencesKey("more_variety_enabled")
    }
}
