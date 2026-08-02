package com.sapglance.core.tips

import com.sapglance.core.settings.PoolAmount
import com.sapglance.core.settings.PoolMix
import java.time.LocalTime
import kotlin.random.Random

/**
 * Pure Kotlin, JVM-testable tip selection logic — no Android imports allowed in this module.
 *
 * Selection is three weighted choices, narrowing each time: which *tier* (practical or tone),
 * which *group* within it (general vs. this hour's pool; motivation vs. philosophy vs.
 * wellbeing), and finally which tip within that group. Anti-repeat is applied to every group
 * up front, before any of those choices — see [pick] and [availableTiers] for why that order is
 * load-bearing rather than incidental.
 *
 * [random] is injected (defaulting to [Random.Default]) purely so tests can supply a
 * deterministic source and assert behavior without relying on statistics. Every draw goes
 * through [weightedPick], which only ever calls `nextInt(bound)`, so a stubbed `Random`
 * returning a fixed roll pins selection exactly.
 */
class TipEngine(
    private val catalog: TipCatalog = TipCatalog.loadDefault(),
    private val random: Random = Random.Default,
) {
    fun dayPartFor(time: LocalTime): DayPart =
        when (time.hour) {
            23 -> DayPart.SLEEP_LATE
            in 0..5 -> DayPart.SLEEP_EARLY_HOURS
            in 6..11 -> DayPart.MORNING
            in 12..17 -> DayPart.AFTERNOON
            else -> DayPart.EVENING // 18..22
        }

    /**
     * [recentTips] are the most recently shown tips, across every call site combined, oldest
     * first. They do double duty: the last [TipHistoryRepository.ANTI_REPEAT_WINDOW] of them are
     * excluded outright so none of them repeats within that span (FR5), and the rest feed the
     * recency weighting in [recencyWeight].
     *
     * [poolMix] is how much of each pool the reader asked for — see [toneChancePercent] and
     * [toneGroups] for the two different things it does. Defaults to [PoolMix.DEFAULT] so every
     * caller (and test) that doesn't know about it yet keeps the default behavior.
     *
     * **A narrow enough [poolMix] can cost the anti-repeat promise**, and this is the one place
     * where the reader can do that to themselves. FR5 ("the last 100 never come back") rests on
     * the reachable set being larger than the window; switch three pools off and the fourth may
     * be smaller than 100, at which point the fallback chain in [pick] runs out and a tip
     * repeats. It degrades rather than failing — nothing crashes and nothing is ever empty — but
     * the promise is the reader's to spend.
     *
     * There used to be a `manual` flag here, distinguishing a widget tap from the passive
     * rotation, and it existed for exactly one reason: the sleep hours were a single fixed
     * message exempt from anti-repeat, so tapping between 23:00 and 05:59 was a silent no-op
     * and had to be redirected to the general pool to visibly do anything. Now that both night
     * windows are real pools, a tap at 2am draws a new night tip like a tap at any other hour,
     * and there is nothing left for the flag to distinguish.
     */
    fun messageFor(
        time: LocalTime,
        recentTips: List<String>,
        poolMix: PoolMix = PoolMix.DEFAULT,
    ): Tip {
        val dayPart = dayPartFor(time)
        // A mix that silences everything is not drawable, and the engine will not crash over a
        // value the settings screen cannot produce but a corrupt or hand-edited DataStore can.
        // Falling back to the default is the least surprising repair: the reader gets the app
        // they installed rather than an error, and the next thing they touch in Settings writes
        // a valid mix back.
        val mix = if (poolMix.isSilent) PoolMix.DEFAULT else poolMix
        return pick(
            preferred = tiersFor(dayPart, mix, exhaustedToneKind(recentTips)),
            withoutToneRunLimit = tiersFor(dayPart, mix, blockedTone = null),
            ignoringPreferences = tiersFor(dayPart, PoolMix.DEFAULT, blockedTone = null),
            recentTips = recentTips,
        )
    }

    /**
     * The tone kind that has just used up its run and must sit this draw out, or null if none has.
     *
     * Anti-repeat already guarantees no *tip* comes back too soon, but it says nothing about
     * *kind*, and three wellbeing lines in a row read as a rut even when all three are different
     * sentences — the pools are distinct voices, and hearing one voice three times running is
     * what makes the rotation feel narrower than it is. So a kind that fills
     * [MAX_CONSECUTIVE_SAME_TONE] consecutive draws yields the next one.
     *
     * Only the three tone kinds are limited. [TipKind.PRACTICAL] deliberately is not: it is the
     * app's default register rather than a voice, it is what the reader asked for by leaving
     * [PoolMix.practical] at [PoolAmount.PLENTY], and capping it would mean forcing tone in at
     * exactly the setting that says not to.
     */
    private fun exhaustedToneKind(recentTips: List<String>): TipKind? {
        if (recentTips.size < MAX_CONSECUTIVE_SAME_TONE) return null
        val run = recentTips.takeLast(MAX_CONSECUTIVE_SAME_TONE).map { catalog.kindOf(it) }
        val candidate = run.first()
        return if (candidate in TONE_KINDS && run.all { it == candidate }) candidate else null
    }

    /**
     * Resolves a tip's full [Tip] (including its citation) from just its displayed text — the
     * form [TipHistoryRepository] persists. Lets callers (e.g. the Settings screen) look up the
     * source of "whatever was last shown" without the history itself needing to store anything
     * beyond plain text.
     */
    fun findByText(text: String): Tip? =
        (
            catalog.general + catalog.morning + catalog.afternoon + catalog.evening +
                catalog.sleepLate + catalog.sleepEarlyHours +
                catalog.tonePools
        ).find { it.text == text }

    /**
     * Just the kind of a tip, from the same plain text the history stores — what
     * [com.sapglance.core.widget.WidgetStyle.forTip] narrows the card's palette by, so a
     * philosophy line doesn't arrive on a bright card.
     *
     * Deliberately not `findByText(text)?.kind`: that concatenates the whole catalog afresh on
     * every call, which is fine for one Settings lookup and wasteful for something resolved
     * again every time a new tip is drawn. This goes through [TipCatalog.kindOf]'s cached map.
     * Null for a text the catalog no longer knows, which callers must treat as "no opinion".
     */
    fun kindOf(text: String): TipKind? = catalog.kindOf(text)

    /** One candidate pool and its share of the draw. */
    private class Group(val weight: Int, val tips: List<Tip>)

    /** The practical-vs-tone split, which [PoolMix.practical] alone controls. */
    private class Tier(val weight: Int, val groups: List<Group>)

    private fun tiersFor(
        dayPart: DayPart,
        mix: PoolMix,
        blockedTone: TipKind?,
    ): List<Tier> {
        val toneChance = toneChancePercent(dayPart, mix.practical)
        return listOf(
            Tier(PERCENT - toneChance, practicalGroups(dayPart)),
            Tier(toneChance, toneGroups(dayPart, mix, blockedTone)),
        )
    }

    /**
     * The practical tier is split between the evergreen [TipCatalog.general] pool and the pool
     * scoped to this hour, by a fixed share rather than by pool size. Concatenating them and
     * drawing uniformly, which is what selection used to do, let file sizes decide the ratio:
     * `general` (41 tips) against a day-part pool of ~23 meant roughly two thirds of practical
     * draws came from the most time-neutral content in the catalog, so the tips that are
     * actually *about* right now were the minority every hour of the day. Nothing intended
     * that; it was an accident of how much had been written for each file, and it would drift
     * again with every content pass.
     *
     * The two night parts are the exception, and take their own pool alone. `general` is written
     * for someone at a desk — stand up, take a walk, break up a long sit — and half of it is the
     * opposite of what 3am calls for, so mixing it in would buy depth by reintroducing exactly
     * the mistimed line [ToneProfile] exists to prevent. What that costs is real and is priced in
     * [toneChancePercent]: night's practical reach is one pool rather than two.
     */
    private fun practicalGroups(dayPart: DayPart): List<Group> =
        when (dayPart) {
            DayPart.MORNING -> dayPartGroups(catalog.morning)
            DayPart.AFTERNOON -> dayPartGroups(catalog.afternoon)
            DayPart.EVENING -> dayPartGroups(catalog.evening)
            DayPart.SLEEP_LATE -> listOf(Group(PERCENT, catalog.sleepLate))
            DayPart.SLEEP_EARLY_HOURS -> listOf(Group(PERCENT, catalog.sleepEarlyHours))
        }

    private fun dayPartGroups(dayPartPool: List<Tip>): List<Group> =
        listOf(
            Group(GENERAL_SHARE_PERCENT, catalog.general),
            Group(PERCENT - GENERAL_SHARE_PERCENT, dayPartPool),
        )

    /**
     * [blockedTone] drops that kind's group outright for this draw. Because [availableTiers]
     * redistributes rather than shrinks, the tone tier keeps its full share and it flows to the
     * other two voices — so the run limit changes *which* tone comes next, never how much tone
     * the user gets. That is the same property that makes an exhausted pool harmless, reused.
     *
     * Each voice's weight is the hour's editorial weighting *scaled by* what the reader asked
     * for, and multiplication is the whole design: a reader's preference bends the profile's
     * shape rather than replacing it. Both zeroes therefore stick. A voice the reader set to
     * [PoolAmount.NONE] never appears, at any hour; and motivation never appears at night no
     * matter what the reader set, because the night profile weights it 0 and nothing multiplies
     * back up from there. The first is a preference and the second is editorial timing, and it
     * is deliberate that the reader cannot overrule the second — see [ToneProfile].
     */
    private fun toneGroups(
        dayPart: DayPart,
        mix: PoolMix,
        blockedTone: TipKind?,
    ): List<Group> {
        val profile = ToneProfile.forDayPart(dayPart)
        return listOf(
            Triple(TipKind.MOTIVATION, profile.motivation * scale(mix.motivation), catalog.motivation),
            Triple(TipKind.PHILOSOPHY, profile.philosophy * scale(mix.philosophy), catalog.philosophy),
            Triple(TipKind.WELLBEING, profile.wellbeing * scale(mix.wellbeing), catalog.wellbeing),
        ).filterNot { (kind, _, _) -> kind == blockedTone }
            .map { (_, weight, pool) -> Group(weight, pool) }
    }

    /**
     * What a reader's [PoolAmount] multiplies a tone voice's profile weight by. Only the ratios
     * between the three matter, which is why "all three at PLENTY" is the same draw as "all three
     * at SOME" — see [PoolMix] for why that is the honest behaviour rather than a rounding
     * artefact.
     */
    private fun scale(amount: PoolAmount): Int =
        when (amount) {
            PoolAmount.NONE -> 0
            PoolAmount.SOME -> 1
            PoolAmount.PLENTY -> 2
        }

    /**
     * How much of a draw the tone tier gets, which [PoolMix.practical] alone decides.
     *
     * [PoolAmount.PLENTY] and [PoolAmount.SOME] are leans in the old sense: both still leave room
     * for the other side, so they read as "mostly this" rather than "only this."
     * [PoolAmount.NONE] is not, and that is the deliberate break with the rule this comment used
     * to state. A reader who turns the practical pool off gets no practical tips, at 100%: the
     * old control could only ever say "less of that, sometimes", which is not an answer to
     * someone who does not want to be told to stand up every hour.
     *
     * The sleep-hours day parts still lean harder towards tone at every level, but no longer for
     * the reason they originally did: night was one fixed message with nothing to rotate, and the
     * daytime split would have shown the identical sentence four nights in five. Night is a real
     * pool now, and the lean survives on two arguments the fixed message was hiding.
     *
     * The first is arithmetic. Night reaches one pool where every other hour reaches two
     * ([practicalGroups]), and that pool is the hardest in the catalog to grow, because a tip has
     * to clear the evidence bar *and* still be worth doing at 3am. Asking 80% of night draws to
     * come from ~14 tips would not actually deliver 80% — anti-repeat would exhaust the pool and
     * redistribute into tone anyway, so the number here would be a claim the engine quietly
     * corrects. Better that the split says what happens.
     *
     * The second is editorial, and is the one that would keep these numbers even if the pool
     * were deep: at 2am a practical instruction is the least welcome register in the app.
     */
    private fun toneChancePercent(
        dayPart: DayPart,
        practical: PoolAmount,
    ): Int =
        when (practical) {
            PoolAmount.NONE -> PERCENT
            PoolAmount.SOME ->
                when (dayPart) {
                    DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS -> NIGHT_TONE_BALANCED_CHANCE_PERCENT
                    else -> TONE_BALANCED_CHANCE_PERCENT
                }
            PoolAmount.PLENTY ->
                when (dayPart) {
                    DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS -> NIGHT_TONE_MINORITY_CHANCE_PERCENT
                    else -> TONE_MINORITY_CHANCE_PERCENT
                }
        }

    /**
     * Anti-repeat is applied to every group *before* any weighted choice, not after choosing one
     * — doing it the other way round (weight first, filter second) let the draw land on a group
     * whose only unseen tips had all just been shown, and repeat one of those while another
     * group still had fresh options sitting right there unused. That was a real bug, and with
     * five groups instead of the original two there are simply more places for it to come back,
     * so the fix is structural rather than a special case per group: [availableTiers] drops
     * anything with nothing fresh to offer, and a dropped group's share is redistributed among
     * the survivors instead of being spent on a forced repeat.
     *
     * Three rules can each empty the board, and they are not equal, so they give way in order of
     * how much they are worth. The tone run limit goes first: it is a preference about *which
     * voice* comes next, and [withoutToneRunLimit] is the same tiers with the blocked voice put
     * back. Anti-repeat goes second, because it is the product promise (FR5) and worth more than
     * a run limit. The reader's own [PoolMix] goes last, and that ordering is the deliberate
     * part: someone who switched the practical pool off would rather see a tone tip they have
     * seen before than a practical one they said they did not want. [ignoringPreferences] is
     * therefore reached only when honouring the mix would mean showing nothing at all — a
     * catalog with an empty pool, which is a test fixture and a corrupt install rather than
     * anything a reader can produce.
     *
     * The order is load-bearing at night and nowhere else. Daytime reaches `general` + an hour's
     * pool + all three tone pools, hundreds of tips against a 100-draw window, so the second
     * fallback is unreachable there. Night reaches one practical pool plus philosophy and
     * wellbeing (motivation is weighted out), and with philosophy blocked for a draw the rest can
     * sit entirely inside the window — the wrong order would spend the promise to keep the
     * preference, on the exact hours where a repeat is most obvious because so little else is on
     * screen.
     */
    private fun pick(
        preferred: List<Tier>,
        withoutToneRunLimit: List<Tier>,
        ignoringPreferences: List<Tier>,
        recentTips: List<String>,
    ): Tip {
        val ages = agesByText(recentTips)
        val blocked = recentTips.takeLast(TipHistoryRepository.ANTI_REPEAT_WINDOW).toSet()
        val available =
            availableTiers(preferred, blocked)
                .ifEmpty { availableTiers(withoutToneRunLimit, blocked) }
                .ifEmpty { availableTiers(withoutToneRunLimit, emptySet()) }
                .ifEmpty { availableTiers(ignoringPreferences, emptySet()) }
        require(available.isNotEmpty()) { "Every tip pool for this day part is empty" }

        val tier = weightedPick(available) { it.weight }
        val group = weightedPick(tier.groups) { it.weight }
        return weightedPick(group.tips) { recencyWeight(it.text, ages) }
    }

    /**
     * Rebuilds [tiers] with only what can actually be drawn right now: every tip in [blocked]
     * removed, then every group left empty (or weighted out entirely, as motivation is at
     * night) dropped, then every tier left with no groups dropped. Because [weightedPick] draws
     * against the sum of whatever survives, dropping is redistribution — a tier keeps its full
     * share when only some of its groups survive, so exhausting the philosophy pool shifts that
     * share to wellbeing rather than quietly shrinking how much tone the user asked for.
     */
    private fun availableTiers(
        tiers: List<Tier>,
        blocked: Set<String>,
    ): List<Tier> =
        tiers.mapNotNull { tier ->
            val groups =
                tier.groups.mapNotNull { group ->
                    val candidates = group.tips.filterNot { it.text in blocked }
                    if (group.weight <= 0 || candidates.isEmpty()) null else Group(group.weight, candidates)
                }
            if (tier.weight <= 0 || groups.isEmpty()) null else Tier(tier.weight, groups)
        }

    /** Draws-ago per tip text: 0 is the most recently shown. Newest occurrence wins. */
    private fun agesByText(recentTips: List<String>): Map<String, Int> {
        val ages = HashMap<String, Int>(recentTips.size)
        recentTips.forEachIndexed { index, text -> ages[text] = recentTips.lastIndex - index }
        return ages
    }

    /**
     * Favors the tips it's been longest since showing, rising linearly from just-out-of-the-
     * window to the far edge of what's remembered; anything never shown at all is treated as
     * maximally overdue.
     *
     * This is the piece that answers "the rotation feels less varied than the pool sizes say it
     * should." Uniform random over the eligible tips is maximum-entropy per draw, but it says
     * nothing about the *gaps between* draws: a tip can leave the anti-repeat window and come
     * straight back on the very next pick while another goes months unseen, and the returns are
     * what a user notices. Weighting by how overdue a tip is turns that into a soft
     * least-recently-used ordering — still random, still able to surprise, but the long tail of
     * neglected tips actually gets used.
     *
     * A shuffled bag (deal the pool in a random permutation, reshuffle when exhausted) was the
     * other candidate and was rejected; see README "Notable design decisions" for why.
     */
    private fun recencyWeight(
        text: String,
        ages: Map<String, Int>,
    ): Int {
        val age = ages[text] ?: return MAX_RECENCY_WEIGHT
        return (age - TipHistoryRepository.ANTI_REPEAT_WINDOW + 1).coerceIn(1, MAX_RECENCY_WEIGHT)
    }

    /**
     * The single point where randomness enters selection, so every choice — tier, group, tip —
     * is one `nextInt` against the summed weights of whatever is actually available.
     */
    private fun <T> weightedPick(
        items: List<T>,
        weight: (T) -> Int,
    ): T {
        var roll = random.nextInt(items.sumOf(weight))
        for (item in items) {
            roll -= weight(item)
            if (roll < 0) return item
        }
        return items.last() // Unreachable while every weight is positive; cheaper than an assert.
    }

    private companion object {
        const val PERCENT = 100

        /**
         * How many draws in a row one tone kind may take before it has to yield to another.
         * Two, so a pair still happens (it reads as a theme) and a third never does (it reads
         * as the app being stuck).
         */
        const val MAX_CONSECUTIVE_SAME_TONE = 2

        /** The kinds the run limit applies to — every kind that is a *voice*, so not PRACTICAL. */
        val TONE_KINDS = setOf(TipKind.MOTIVATION, TipKind.PHILOSOPHY, TipKind.WELLBEING)

        /**
         * The tone tier's share of a draw at each [PoolAmount] of practical, during waking hours.
         * [PoolAmount.NONE] is not here because it is [PERCENT] by definition rather than by
         * tuning.
         *
         * There used to be a third tuned point, 80 here and 85 at night, for the old
         * `VarietyLevel.PLAYFUL`. It went with the three-position control and was not replaced:
         * three amounts cannot carry four points, and of the two candidates for the top of the
         * ladder, "no practical tips at all" is the one a reader can state as a preference and
         * "practical tips 20% of the time" is the one they cannot tell apart from 15%. Anyone
         * wanting the old PLAYFUL is one step away in either direction.
         */
        const val TONE_BALANCED_CHANCE_PERCENT = 50
        const val TONE_MINORITY_CHANCE_PERCENT = 20

        /** The same, for 23:00-05:59 — see [toneChancePercent] for why night is different. */
        const val NIGHT_TONE_BALANCED_CHANCE_PERCENT = 70
        const val NIGHT_TONE_MINORITY_CHANCE_PERCENT = 50

        /**
         * The evergreen `general` pool's share of the practical tier, the rest going to the
         * pool for the current day part. An even split rather than the size-proportional one
         * that concatenating the two pools used to produce implicitly.
         */
        const val GENERAL_SHARE_PERCENT = 50

        /**
         * The most a tip can be favored for being overdue, relative to one that only just left
         * the anti-repeat window. This is the whole span [TipHistoryRepository] remembers beyond
         * the window, so the weighting uses every bit of recency signal that exists rather than
         * an arbitrary cut-off — and it degrades to plain uniform random on a fresh install,
         * where nothing has been shown yet and every tip is equally overdue.
         *
         * Floored at 1 so that shrinking the history back down to the window (which would leave
         * no signal at all) degrades to uniform random rather than throwing on an empty range.
         */
        val MAX_RECENCY_WEIGHT =
            (TipHistoryRepository.MAX_RECENT_TIPS - TipHistoryRepository.ANTI_REPEAT_WINDOW)
                .coerceAtLeast(1)
    }
}
