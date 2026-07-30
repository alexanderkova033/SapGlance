package com.sapglance.app.widget.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sapglance.app.R
import com.sapglance.app.SapGlanceApp
import com.sapglance.app.settings.presentation.SettingsActivity
import com.sapglance.core.tips.model.TipKind
import com.sapglance.core.widget.model.WidgetStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

/**
 * FR1: shows the current tip. Tapping the card itself gets a new tip on the spot (via
 * [RefreshTipAction], without opening the app); the small gear icon in the corner is the
 * only way into the settings screen — AppWidgets can't intercept long-press, the launcher
 * reserves that gesture for its own move/resize/remove UI, so a dedicated tap target is the
 * only reliable option. [WidgetRefreshWorker] is what normally advances the tip on a timer;
 * this only computes a fallback tip itself on the very first render (e.g. right after install,
 * before any worker has run yet), reusing the same persisted "last tip" every other trigger
 * reads and writes so the anti-repeat guarantee (FR5) holds across all of them. Whichever trigger
 * advances the tip, the widget repaints by *observing* the persisted history rather than by
 * being handed a value — see the comment inside [provideGlance] for why that distinction is
 * what makes the repaint happen at all.
 */
class TipWidget : GlanceAppWidget() {
    // Exact, not Responsive. Responsive composes once per *declared* bucket and the launcher
    // picks the largest bucket that fits — which quietly wastes whatever space falls between
    // buckets. A real 154x183dp card on a 4-column phone matched only the 110x110 bucket
    // (every wider bucket was too wide to fit), so a third of its height went unused and it
    // drew the cramped small-card layout: 10sp type, no quote glyph, and a lot of nothing.
    // [metricsFor] already derives every dimension from the size continuously, so buckets add
    // nothing but rounding error. Exact costs a recomposition per resize, which is cheap for a
    // card this simple and only happens while the user is actually dragging a handle.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val container = (context.applicationContext as SapGlanceApp).container
        // Read once, synchronously, purely so the very first frame has something to draw
        // (and so a fresh install, with no history yet, still gets a tip). Everything after
        // that comes from the flow collected inside provideContent below.
        val initialTip =
            container.tipHistoryRepository.recentTips.first().lastOrNull()
                ?: run {
                    val varietyLevel = container.settingsRepository.settings.first().varietyLevel
                    container.advanceTip(LocalTime.now(), varietyLevel = varietyLevel).text
                }
        // The card labels each tip with its kind, and the history stores nothing but text, so the
        // kind has to be looked up in the catalog on every repaint. Forcing the parse here, in a
        // suspend context that is already waiting on DataStore anyway (and usually behind the
        // warm-up started at process start), keeps it off the composition below — after this,
        // TipEngine is a plain field read and `kindOf` is a map lookup.
        container.warmUp()

        provideContent {
            // The tip MUST be observed as Compose state from inside provideContent, not read
            // into a local above it. provideGlance runs exactly once per Glance *session*, not
            // once per updateAll() — a session outlives many refreshes. A tip captured above
            // would therefore be frozen for the session's whole lifetime: updateAll() only
            // refreshes AppWidgetSession's own `glanceState`/`options` state holders, so a
            // composition that reads neither has no changed snapshot state, never recomposes,
            // and never emits new RemoteViews for the host to draw. That was the real cause of
            // "the tip data updates but the widget doesn't repaint" — confirmed on-device with
            // the widget stuck three tips behind DataStore while its RemoteViews instance never
            // changed. Collecting the flow here makes the repaint happen the moment a new tip
            // is persisted, whichever trigger wrote it.
            val tipFlow =
                remember {
                    container.tipHistoryRepository.recentTips
                        .map { it.lastOrNull() ?: initialTip }
                        // dataStore.data emits on *every* preference write, including the
                        // unrelated screen-on tick counter the refresh worker bumps; without
                        // this each one would cost a pointless recomposition and RemoteViews push.
                        .distinctUntilChanged()
                }
            val tip by tipFlow.collectAsState(initial = initialTip)
            // Keyed on the tip, so a new tip relabels the card in the same recomposition that
            // retexts it — the label and the words must never disagree, even for a frame.
            val kind = remember(tip) { container.tipEngine.kindOf(tip) }

            GlanceTheme {
                TipWidgetContent(tip, kind, WidgetStyle.forTip(tip))
            }
        }
    }
}

/**
 * Every color the card draws text and chrome in, for one direction of contrast.
 *
 * The widget used to hardcode white text with a translucent *black* wash behind it, which
 * silently assumed every background would stay dark — and did, for as long as they all were.
 * The moment a pale style exists that assumption produces white-on-cream: technically rendered,
 * practically invisible. So the whole ink set flips together rather than the text color alone;
 * a light card needs a *lighter* wash than its background (not a darker one), a dark card
 * frame instead of a white one, and a dark gear glyph on a bright button instead of the
 * reverse. Flipping only some of those is what leaves a card looking half-inverted.
 */
private enum class WidgetInk(
    val text: Color,
    val scrim: Color,
    val quoteMark: Color,
    val footer: Color,
    val settingsButtonRes: Int,
    val settingsGlyph: Color,
) {
    ON_DARK(
        text = Color.White,
        // Held at exactly the alpha the old text panel used, because the contrast arithmetic
        // under the words is unchanged by spreading the same wash over the whole card — every
        // pixel of artwork the text crosses is composited against the same 0.42 as before. What
        // *did* change is everything the text doesn't cross, which is now toned to match rather
        // than framing a darker rectangle. Anything shallower would trade real legibility for
        // a brighter card; see [TipWidgetContent] for why the rectangle went away.
        scrim = Color.Black.copy(alpha = 0.42f),
        quoteMark = Color.White.copy(alpha = 0.6f),
        footer = Color.White.copy(alpha = 0.8f),
        settingsButtonRes = R.drawable.widget_settings_button_bg,
        settingsGlyph = Color.White.copy(alpha = 0.9f),
    ),

    // Not pure black: against a bright card, #000 has a hard glare-y edge that a near-black
    // with a trace of the background's own warmth doesn't.
    //
    // The wash goes white here, and much shallower than the 0.70 the old panel needed. A panel
    // had to flatten the artwork it covered into something uniform; a whole-card wash only has
    // to lift the *darkest* thing the near-black text can cross, and on these styles that is
    // Meadow's grass and Blossom's branch, which measure 6.4:1 against this ink with no wash at
    // all. 0.30 takes the worst of them to 9.7:1 and still leaves the pale styles their color —
    // at 0.70 a whole-card wash bleaches Blossom's blush out of the card entirely.
    ON_LIGHT(
        text = Color(0xFF17181C),
        scrim = Color.White.copy(alpha = 0.30f),
        quoteMark = Color(0xFF17181C).copy(alpha = 0.5f),
        footer = Color(0xFF17181C).copy(alpha = 0.8f),
        settingsButtonRes = R.drawable.widget_settings_button_bg_light,
        settingsGlyph = Color(0xFF17181C).copy(alpha = 0.82f),
    ),
}

/**
 * The drawable *and* the ink for a style, resolved in one `when` on purpose: they are a matched
 * pair, and a background whose ink says the opposite of its artwork is unreadable rather than
 * merely ugly. Adding a `WidgetStyle` entry breaks this exhaustive `when` at compile time, so
 * a new style cannot ship having picked one and forgotten the other.
 */
private fun WidgetStyle.skin(): Pair<Int, WidgetInk> =
    when (this) {
        WidgetStyle.FOREST -> R.drawable.widget_quote_background to WidgetInk.ON_DARK
        WidgetStyle.OCEAN -> R.drawable.widget_background_ocean to WidgetInk.ON_DARK
        WidgetStyle.SUNSET -> R.drawable.widget_background_sunset to WidgetInk.ON_DARK
        WidgetStyle.MIDNIGHT -> R.drawable.widget_background_midnight to WidgetInk.ON_DARK
        WidgetStyle.AURORA -> R.drawable.widget_background_aurora to WidgetInk.ON_DARK
        WidgetStyle.DAWN -> R.drawable.widget_background_dawn to WidgetInk.ON_DARK
        WidgetStyle.RAIN -> R.drawable.widget_background_rain to WidgetInk.ON_DARK
        WidgetStyle.WINTER -> R.drawable.widget_background_winter to WidgetInk.ON_LIGHT
        WidgetStyle.PAPER -> R.drawable.widget_background_paper to WidgetInk.ON_LIGHT
        WidgetStyle.MEADOW -> R.drawable.widget_background_meadow to WidgetInk.ON_LIGHT
        WidgetStyle.BLOSSOM -> R.drawable.widget_background_blossom to WidgetInk.ON_LIGHT
    }

/**
 * The word the card puts above a tip.
 *
 * [TipKind.PRACTICAL] is labelled here even though the settings card deliberately leaves it
 * unlabelled: on that card the day part already occupies the same line and tagging the majority
 * kind alongside it would be noise, whereas on the widget the label is the *only* thing naming
 * the tip, and one that appeared for three kinds out of four would read as an omission rather
 * than as a distinction.
 *
 * "Health" rather than "Practical" because the label answers "what is this about", which is what a
 * reader glancing at it wants, not "which register is it written in". It also keeps the word clear
 * of the *variety setting* named "Practical" — one word meaning two different things across two
 * screens is worse than two words meaning one thing.
 */
private fun TipKind.labelRes(): Int =
    when (this) {
        TipKind.PRACTICAL -> R.string.tip_kind_health
        TipKind.MOTIVATION -> R.string.tip_kind_motivation
        TipKind.PHILOSOPHY -> R.string.tip_kind_philosophy
        TipKind.WELLBEING -> R.string.tip_kind_wellbeing
    }

/** The card's own corner. The tip is laid on the card now, so nothing has to nest inside it. */
private val CARD_CORNER_RADIUS = 20.dp

/** Gap under the `❝` glyph, charged in [metricsFor] and drawn in [TipWidgetContent]. */
private val QUOTE_MARK_GAP = 2.dp

/**
 * Tip font sizes, smallest first. The layout picks the largest one whose worst case still fits
 * the card rather than mapping size ranges to a font by hand, which is what left a tall card
 * holding type sized for a short one.
 *
 * The rungs are close together on purpose. A ladder only ever rounds *down* — the card gets the
 * largest size that fits and forfeits the remainder — so a gap between rungs is type the card
 * had room for and didn't use. The old 11→13→15→18→21 tail had gaps of up to 18%, and a 3x2
 * card landed on 13sp with enough spare height for 17sp. Filling in the missing sizes costs one
 * extra iteration of a loop over a dozen elements and hands that space back.
 */
private val TIP_FONT_LADDER =
    listOf(
        6.sp, 7.sp, 8.sp, 9.sp, 10.sp, 11.sp, 12.sp, 13.sp,
        14.sp, 15.sp, 16.sp, 17.sp, 18.sp, 20.sp, 22.sp, 24.sp,
    )

/**
 * The height of one line box, as a multiple of the *rendered* font height.
 *
 * Measured, not assumed: a 16sp tip on the test device draws its lines 20.9dp apart, and Noto
 * Serif's own metrics (ascent 0.93em, descent 0.25em, zero leading) say the same 1.19. This was
 * 1.25 for a long time, which is a plausible-looking number that belongs to no particular
 * font — it happened to over-reserve here, which is the harmless direction, but the point of
 * measuring is that neither direction is then a matter of luck.
 *
 * Glance 1.1.1's `TextStyle` has no `lineHeight`, so this is a reading of what the platform
 * does rather than a value the card gets to set. If a future Glance adds one, this constant and
 * that call have to agree.
 */
private const val LINE_HEIGHT_RATIO = 1.19f

/**
 * How wide the *widest* kind label is, as a multiple of its font size, uppercased and measured
 * against the real NotoSerif-Bold.ttf: `MOTIVATION` 6.87em, `PHILOSOPHY` 6.82, `WELLBEING` 6.31,
 * `HEALTH` 4.35. Shipped 3% over the widest, the same allowance [TipFace.minColumnRatio] carries,
 * to cover the small disagreement between desktop font metrics and Android's hinted advances.
 *
 * Sized against the widest label rather than the one being drawn, on purpose: the label is beside
 * the settings button, so the size it can take is a question about the *card*, and answering it
 * per tip would resize the card's top line on every refresh — `HEALTH` would sit a rung larger
 * than `PHILOSOPHY` on exactly the cards where the difference is most visible.
 */
private const val KIND_LABEL_WIDTH_RATIO = 7.1f

/**
 * Below this the label stops being information and becomes a smudge, so a card too narrow to
 * hold [KIND_LABEL_WIDTH_RATIO] at this size goes without one. Only the very narrowest slots
 * reach it: a 110dp-wide card that is also tall enough for the full-size settings button.
 */
private const val MIN_KIND_LABEL_SP = 7f

/**
 * A typeface together with the fitting figure measured for *that* face.
 *
 * These are one type rather than two constants because they are one decision. They were separate
 * once, and the predictable happened: the face changed from serif to condensed and the width
 * figure — measured against the serif — stayed behind. Nothing broke loudly, since a stale figure
 * over-reserves rather than clipping, but the card silently sized its type for a font it was no
 * longer using and gave back a third of its own height. Pairing them means changing the face
 * without re-measuring is not something you can forget to do; it is something you cannot express.
 *
 * [minColumnRatio] is the narrowest text column, as a multiple of the rendered font height, in
 * which *every* tip in the catalog still wraps to at most [MAX_TIP_LINES] lines. It replaced a
 * "characters × average character width" estimate, which was the wrong shape of answer twice
 * over: a per-character average can only describe a typical tip, so the tips that wrap worst
 * than typical were exactly the ones it under-served, and the fix for that was a spare line of
 * height that every card then paid for.
 *
 * To re-measure: load the face and greedily wrap every line of `core/src/main/resources/tips`
 * at a range of column widths, and take the widest column any single tip needs to come in at
 * [MAX_TIP_LINES] lines. Greedy is the safe algorithm to measure with — Android's TextView
 * defaults to a balanced line breaker, which never uses *more* lines than greedy does. Round up:
 * over-reserving costs a rung of type, under-reserving truncates a sentence.
 */
private data class TipFace(
    val family: FontFamily,
    val minColumnRatio: Float,
)

/**
 * The face the tip is set in.
 *
 * `FontFamily` accepts any family the platform resolves, not only the four Glance predefines, but
 * most of the names Android advertises are aliases: on the test device `georgia`, `times`,
 * `baskerville`, `palatino` and `goudy` all collapse to Noto Serif, and `arial`/`helvetica`/
 * `tahoma` to Roboto. The genuinely distinct faces are `serif`, `sans-serif`,
 * `sans-serif-condensed` (Roboto at `wdth` 75), `sans-serif-black` (Roboto 900),
 * `source-sans-pro`, `sans-serif-smallcaps` (Carrois Gothic SC), `serif-monospace` (Cutive Mono),
 * `casual` (Coming Soon) and `cursive` (Dancing Script).
 *
 * Noto Serif Bold. A monospaced typewriter was tried here and is the reason this doc is long,
 * because it failed for a reason that is not obvious until you see it: in a fixed-width face the
 * space character is as wide as an `m`, so the gaps between words are roughly two and a half
 * times a proportional font's and the text reads as though it has been pulled apart. That is not
 * tunable; it is what monospace *is*. It also cost size twice over, since every glyph takes the
 * width of the widest, so fewer characters fit per line and more lines are needed for the same
 * tip.
 *
 * Two constraints eliminated everything more decorative. Every face on the device with a *real*
 * bold (`serif`, `sans-serif`, `source-sans-pro`) is a plain one, and every characterful face
 * (Cutive Mono, Coming Soon, Dancing Script, Carrois Gothic SC) is Regular-only — those all read
 * as too thin over the gradients, and synthesized bold does very little for them. Widgets are
 * RemoteViews and cannot use an app-bundled font, so that is the whole menu. Worth knowing too:
 * `monospace` does not even resolve on this device, falling back silently to the proportional
 * default despite `/system/etc/fonts.xml` defining it, while `serif-monospace` renders correctly.
 *
 * A serif carries more character than the grotesques, has a genuinely drawn bold, and spaces its
 * words tightly. That is the whole of the reasoning.
 */
private val TIP_FACE =
    TipFace(
        family = FontFamily.Serif,
        // Measured against the real NotoSerif-Bold.ttf off the device, over the catalog as it
        // stood at 282 tips (it is 312 now, and the widest line added since is narrower than
        // several that were already in the measured set, so the figure below still holds): the
        // widest-wrapping line in the catalog ("Morning movement outdoors does double duty…")
        // needs 8.69 line-heights of column to come in at seven lines, and the next few crowd
        // just under it. Shipped 3% over that, which covers the small disagreement between a
        // measurement made with desktop font metrics and Android's own hinted advances — checked
        // against three on-device renders, where the two differ by under 2%.
        minColumnRatio = 8.95f,
    )

/**
 * How much card there is to spend, per size bucket.
 *
 * Note carefully what this does *not* do: it does not measure the tip. An earlier
 * measure-and-fit attempt predicted wrapping with a StaticLayout measurement against
 * `LocalSize.current` and picked a font size per tip, which was unreliable — that estimate is
 * only ever a guess at the width the real RemoteViews TextView gets on the actual home screen,
 * and the two disagree across launchers and grid rounding. When they did, the guess was wrong
 * in both directions: too large for long tips (clipped past `maxLines`) and too small for short
 * ones (one word per line). That approach stays rejected.
 *
 * Keying off the *card's* size instead has none of that failure mode. The size is given to us
 * rather than predicted, and it doesn't vary per tip, so the same tip always renders identically
 * at a given widget size — nothing can drift out of sync with reality, and nothing shifts
 * underneath the reader when the tip changes. Real text wrapping still decides the line count;
 * this only decides how much room wrapping gets to happen in.
 *
 * The decorative chrome is what gives way first as the card shrinks, because it is the only part
 * that isn't the point. Chrome is charged before the type is sized, so every dp of it is a dp the
 * ladder cannot spend. The app-name footer is charged at every size and never dropped — a card
 * with no name on it reads as unfinished rather than minimal, and the label is cheap next to the
 * `❝` glyph, which is held back for cards tall enough to afford it.
 */
private data class CardMetrics(
    val tipFontSize: TextUnit,
    val maxTipLines: Int,
    val showQuoteMark: Boolean,
    val quoteMarkSize: TextUnit,
    /** `null` on a card too narrow to hold the longest label legibly — see [KIND_LABEL_WIDTH_RATIO]. */
    val kindLabelFontSize: TextUnit?,
    val footerFontSize: TextUnit,
    val footerInset: Dp,
    val textMargin: Dp,
    val settingsTapSize: Dp,
    val settingsInset: Dp,
    val settingsButtonSize: Dp,
    val settingsGlyphSize: Dp,
)

/**
 * Derived from the size rather than matched against buckets, so any size the host hands back
 * lands somewhere sensible instead of falling into a `when` branch that had to guess.
 *
 * The two axes are treated separately because they constrain different things, and conflating
 * them is what made the first version of this too rigid:
 *
 * - **Width sets the font size**, because width is what decides how a tip wraps. A column narrower
 *   than [TipFace.minColumnRatio] line-heights turns the longest tips into more lines than a
 *   glance is, and eventually into more lines than the card can show at all.
 * - **Height sets how much decoration survives**, because the `❝` glyph and the app-name footer
 *   cost fixed vertical space that a short card needs for the tip itself.
 *
 * `maxTipLines` then falls out of the arithmetic rather than being another hand-tuned constant:
 * whatever height is left after chrome, divided by the line height the font implies. Note this is
 * still not measuring the *text* — it never asks how a particular tip will wrap, which is the
 * prediction that failed before. It only asks how many lines of any text this card can display.
 *
 * [fontScale] is the system font-size setting, and every `sp` here has to be multiplied through
 * it before it can be compared against a `dp`. Leaving it out is not a rounding error: the test
 * device sits at 1.1, so a card that believed it was drawing 16sp of type was really drawing
 * 17.6dp of it, under-reserved every height by a tenth, and finished 3dp short of the settings
 * button. At the 1.3 an accessibility setting can ask for, the same arithmetic truncated tips.
 */
private fun metricsFor(
    size: DpSize,
    fontScale: Float,
): CardMetrics {
    fun TextUnit.lineHeight(): Dp = (value * fontScale * LINE_HEIGHT_RATIO).dp

    // Short cards give up the quote glyph, but never the name: an unbranded card reads as an
    // empty one. The footer instead shrinks with the card, which costs far less height than the
    // glyph does and keeps the widget identifiable at every size.
    val compact = size.height < 140.dp

    // The glyph has to earn its height, and the bar has risen twice. It began at 170dp, where the
    // default 2x2 (~154x183dp) drew it and the ~26dp it costs — 14% of that card — pushed the tip
    // two rungs down the ladder. That is the hollow failure exactly: the ornament ends up the
    // largest thing on the card and the sentence the smallest. 200dp fixed that card and not the
    // next one up; on a real 187x226dp 2x2 the glyph was still costing the difference between
    // 16sp and 18sp. Past 250dp there is genuinely room for both.
    val showQuoteMark = size.height >= 250.dp
    val quoteMarkSize = if (size.height >= 290.dp) 24.sp else 20.sp

    // The tip's margin from the card edge — one margin now, where there used to be two nested
    // ones. It is charged once against the column width instead of twice, which is what pays for
    // the roomier figure: the old 8dp card padding plus 7dp panel padding came to the same 15dp
    // of edge while leaving the text 7dp from a visible panel border, i.e. reading as crammed
    // into a box rather than set on a card.
    val textMargin =
        when {
            compact -> 8.dp
            size.height < 240.dp -> 14.dp
            else -> 18.dp
        }
    val footerFontSize =
        when {
            compact -> 8.sp
            size.height < 210.dp -> 10.sp
            else -> 11.sp
        }
    val footerInset =
        when {
            compact -> 6.dp
            size.height < 240.dp -> 10.dp
            else -> 12.dp
        }
    // Two sizes, not one: the circle is what the eye has to find and the box is what the thumb
    // has to hit, and those wanted opposite things. A 34dp circle that was also its own tap
    // target read as the loudest thing on the card while still landing under Material's 48dp
    // minimum — 28dp on screen once the launcher's own 0.83 scale is applied. Splitting them
    // makes the mark quieter *and* the target half again bigger; the extra area is invisible and
    // costs the tip nothing, since only the drawn circle is charged as chrome below.
    val settingsButtonSize =
        when {
            compact -> 18.dp
            size.height < 200.dp -> 24.dp
            else -> 28.dp
        }
    val settingsTapSize =
        when {
            compact -> 26.dp
            size.height < 200.dp -> 40.dp
            else -> 48.dp
        }
    // The circle rides in the corner of that box rather than the middle of it, which is worth
    // two things. It hands the tip back the difference — the rail below is charged from where
    // the circle ends, not where the target does — and it happens to be where the card's own
    // corner has most room: at half the card's 20dp radius the button is concentric with the
    // corner arc, so the gap between them is even the whole way round instead of pinching at
    // 45°. Further in *or* further out both crowd it.
    val settingsInset =
        when {
            compact -> 4.dp
            size.height < 200.dp -> 5.dp
            else -> 6.dp
        }

    // The kind label shares the card's top edge with the settings button, so it is charged the
    // button's whole rail at *both* ends: it stays centred on the card — matching the footer below
    // it and the tip between them — which it could not do if the clearance came off one side only.
    // Then it takes whatever that leaves, capped at the footer's size because it is the footer's
    // counterpart rather than a headline. A card too narrow for the longest label at a legible
    // size draws none, the same bargain the `❝` glyph makes with height.
    val kindLabelBudget = size.width - maxOf(textMargin, settingsInset + settingsButtonSize) * 2
    val kindLabelSp =
        minOf(
            footerFontSize.value,
            kindLabelBudget.value / (fontScale * KIND_LABEL_WIDTH_RATIO),
        )
    val kindLabelFontSize = if (kindLabelSp >= MIN_KIND_LABEL_SP) kindLabelSp.sp else null

    // What the tip has to stay clear of, at *both* ends, because it is centred in the whole card:
    // room left only at the top would move the centre, and the card reading as top-heavy is the
    // defect this replaced. So one rail, sized to whichever corner overlay is taller, charged
    // twice. Note it is the drawn circle that is charged and not the tap box: an invisible target
    // overlapping the first line of a long tip harms nothing, while reserving 48dp at both ends
    // of a 226dp card would cost two rungs of type.
    val rail =
        maxOf(
            settingsInset + settingsButtonSize,
            footerInset + footerFontSize.lineHeight(),
            // The label is inset like the footer, so the two small-caps lines frame the tip at
            // equal distances. It has never been the tallest of the three — the settings circle
            // is, at every size — so today it costs the tip nothing at all; charged here anyway so
            // that stays true by arithmetic rather than by luck if the button ever shrinks.
            footerInset + (kindLabelFontSize?.lineHeight() ?: 0.dp),
        )
    val quoteMarkHeight = if (showQuoteMark) quoteMarkSize.lineHeight() + QUOTE_MARK_GAP else 0.dp
    val available = size.height - rail * 2 - quoteMarkHeight
    val column = size.width - textMargin * 2

    // Largest font that clears both constraints, rather than a hand-drawn map from width ranges
    // to font sizes. That map had no way to notice spare *height*, so a narrow-but-tall card —
    // the shape a 2-column phone slot actually produces — got type sized for a short card and
    // looked half empty.
    //
    // The first condition is the one that guarantees the line count, and it is a measurement of
    // the catalog rather than an estimate of a tip (see [TipFace.minColumnRatio]); the second
    // only asks whether that many lines fit. Both are needed: height alone would happily allow
    // nine lines of small type, which is a paragraph rather than a glance.
    val fontSize =
        TIP_FONT_LADDER.lastOrNull { candidate ->
            column >= (candidate.value * fontScale * TIP_FACE.minColumnRatio).dp &&
                candidate.lineHeight() * MAX_TIP_LINES <= available
        } ?: TIP_FONT_LADDER.first()

    return CardMetrics(
        tipFontSize = fontSize,
        // Whatever the card can physically show, which the font choice above has already made at
        // least [MAX_TIP_LINES]. Deliberately not clamped to that number: it is a target for
        // choosing type, never a knife to cut a sentence with. See [MAX_TIP_LINES].
        maxTipLines = (available / fontSize.lineHeight()).toInt().coerceAtLeast(MIN_TIP_LINES),
        showQuoteMark = showQuoteMark,
        quoteMarkSize = quoteMarkSize,
        kindLabelFontSize = kindLabelFontSize,
        footerFontSize = footerFontSize,
        footerInset = footerInset,
        textMargin = textMargin,
        settingsTapSize = settingsTapSize,
        settingsInset = settingsInset,
        settingsButtonSize = settingsButtonSize,
        settingsGlyphSize = settingsButtonSize * 0.55f,
    )
}

/** Floor so a tiny card still shows a couple of lines. */
private const val MIN_TIP_LINES = 2

/**
 * How many lines a tip may wrap to. Seven: past that a glance turns into a paragraph, and the
 * card stops being read and starts being skimmed.
 *
 * **A target for choosing the font, never a limit on what gets drawn.** This distinction has been
 * got wrong twice, in both directions, so it is worth stating plainly. The first version was a
 * flat `maxLines` applied after the size had been chosen, and it cut the ends off tips at roughly
 * 1,700 sizes across the declared range. The second constrained font selection — correctly — but
 * *also* clamped the rendered `maxLines` to the same number, which quietly reintroduced the
 * clipping for any tip that wrapped worse than the average the estimate was built on.
 *
 * Six was the number while the fit was an estimate, and the estimate is what made six unreliable:
 * a per-character average said six, real wrapping delivered seven for the longest tips, and the
 * card had to carry a spare line of height to survive the difference. Now that the fit is
 * measured across the whole catalog (see [TipFace.minColumnRatio]), seven is simply what the
 * longest tips have always taken on the default card — the constant describes the render instead
 * of contradicting it, and admitting that is worth two rungs of type size.
 *
 * [TIP_FONT_LADDER] runs down to 6sp so the target stays reachable on the narrowest declared
 * card, which cannot fit 90 characters into seven lines at any larger size.
 */
private const val MAX_TIP_LINES = 7

/**
 * [kind] is `null` for a tip the catalog no longer recognises — a line that has since been reworded
 * or dropped can still be sitting in a user's history — in which case the card simply goes
 * unlabelled. An unlabelled tip is fine; a confidently mislabelled one would not be.
 */
@Composable
private fun TipWidgetContent(
    tip: String,
    kind: TipKind?,
    style: WidgetStyle,
) {
    // Glance has its own LocalContext (androidx.glance), distinct from Compose UI's — this
    // version of glance-appwidget's actionStartActivity only takes an Intent, there's no
    // reified actionStartActivity<Activity>() convenience overload.
    val context = LocalContext.current

    // The card's own artwork decides the ink, not the phone's day/night setting: this is a
    // fixed-palette quote card (à la the "Motivation" app), so what the text has to contrast
    // against is the style behind it, which the home screen's theme says nothing about. A
    // style-driven flip is also the only one that stays correct when the tip — and with it the
    // background — changes underneath a widget nobody is looking at.
    val (backgroundRes, ink) = style.skin()
    val metrics = metricsFor(LocalSize.current, context.resources.configuration.fontScale)

    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(CARD_CORNER_RADIUS)
                .background(ImageProvider(backgroundRes))
                // Whole-card tap: refreshes the tip in place. The settings icon below has its
                // own clickable modifier, which takes the tap over this one within its bounds.
                .clickable(actionRunCallback<RefreshTipAction>()),
        contentAlignment = Alignment.Center,
    ) {
        // The surface the tip is read against, and the whole card is it.
        //
        // This was a rounded panel drawn just behind the words, and the panel is what made the
        // card look wrong. It is worth being specific about why, because "add a surface for
        // contrast" was the right instinct and only the shape of it was wrong. A panel sized to
        // its text is a second, competing rectangle inside a rounded card: its corners fight the
        // card's own, it slices whatever artwork it crosses in half, it changes size on every
        // refresh, and — measured on the test device — its edge came within 2dp of the settings
        // button. Worse, it charged the horizontal margin twice, once as card padding and once as
        // its own padding, so the words ended up 7dp from a visible border with no room to
        // breathe, while a short tip drew a small box adrift in a large empty card.
        //
        // Spreading the same wash over the whole card fixes all of that at once and costs nothing
        // in contrast: the pixels under the text are composited against exactly the alpha they
        // were before (see [WidgetInk]). What it buys is a single margin instead of two nested
        // ones — which is a rung of type size handed back — one shape instead of three, and
        // artwork that reads as artwork rather than as a frame around a box.
        Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(ink.scrim))) {}

        // Centred in the *whole* card, not in what is left after the footer. Both the footer and
        // the settings button are corner overlays rather than rows in a column, so this block
        // gets the full height and its centre is the card's centre; the clearance either side is
        // reserved as a rail in [metricsFor] rather than being left to luck.
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = metrics.textMargin),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Dropped outright on a compact card. It is the single most expensive piece of
                // pure decoration in the layout, and a tip nobody can read costs more than a
                // missing flourish.
                if (metrics.showQuoteMark) {
                    Text(
                        text = "❝",
                        style =
                            TextStyle(
                                fontSize = metrics.quoteMarkSize,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontFamily = FontFamily.Serif,
                                color = ColorProvider(ink.quoteMark),
                            ),
                    )
                    Spacer(GlanceModifier.height(QUOTE_MARK_GAP))
                }
                Text(
                    text = tip,
                    maxLines = metrics.maxTipLines,
                    // Load-bearing for the centring, not decoration. `textAlign` centres lines
                    // within the *TextView's own* measured width, and a wrap-content TextView
                    // inside a Box (a FrameLayout, gravity start) is exactly as wide as its
                    // longest line — so a tip short enough not to wrap would centre inside itself
                    // and then sit flush left, looking un-centred in the one case where the
                    // centring is most obvious. Filling the width makes the two agree.
                    modifier = GlanceModifier.fillMaxWidth(),
                    style =
                        TextStyle(
                            fontSize = metrics.tipFontSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TIP_FACE.family,
                            // Centred, which is the quote-card reading of this rather than the
                            // typographic one. Worth knowing what it costs, since ranging the
                            // text left was tried and rejected here: at up to seven lines,
                            // centring leaves both edges ragged and can strand a two-word last
                            // line mid-card. It is a deliberate trade of that for the symmetry.
                            textAlign = TextAlign.Center,
                            color = ColorProvider(ink.text),
                        ),
                )
            }
        }

        // What kind of thing this tip is, in the top rail opposite the settings button. Worth the
        // line because the four kinds ask genuinely different things of the reader: a health
        // finding is something to act on, a philosophy line something to sit with, and knowing
        // which before reading changes how the sentence lands. It also quietly explains the
        // variety setting — a card that says PHILOSOPHY is showing the user what that slider did.
        //
        // Set in the same face and ink as the app name below, so the two read as a matched pair
        // framing the tip, but bold where the footer is medium: this describes the card's content
        // and the footer is a byline, and at these sizes weight is the only distinction available
        // (Glance's TextStyle has no letterSpacing, so the settings screen's tracking can't be
        // mirrored here).
        val kindLabelRes = kind?.labelRes()
        val kindLabelFontSize = metrics.kindLabelFontSize
        if (kindLabelRes != null && kindLabelFontSize != null) {
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(top = metrics.footerInset),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = context.getString(kindLabelRes).uppercase(),
                    // Clipping a label is survivable; wrapping one is not. A second line would fall
                    // out of the rail reserved for it and land on the tip's first line, so if
                    // [KIND_LABEL_WIDTH_RATIO] is ever a hair optimistic this fails narrowly
                    // instead of overlapping the thing the card exists to show.
                    maxLines = 1,
                    style =
                        TextStyle(
                            fontSize = kindLabelFontSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = TIP_FACE.family,
                            textAlign = TextAlign.Center,
                            color = ColorProvider(ink.footer),
                        ),
                )
            }
        }

        // Shown at every size. An earlier version dropped this on small cards to buy height,
        // which made the widget look unfinished rather than minimal — a card with no name on it
        // reads as empty. Scaling the label down costs a few dp where hiding it saved about
        // twenty, and that difference is affordable.
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(bottom = metrics.footerInset),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                // Derived from the centralized app_name resource (rather than a second hardcoded
                // string) so changing the product name only ever means editing strings.xml.
                text = context.getString(R.string.app_name).uppercase(),
                style =
                    TextStyle(
                        fontSize = metrics.footerFontSize,
                        fontWeight = FontWeight.Medium,
                        // Same face as the tip, so the card reads as one thing. The `❝` glyph
                        // above deliberately stays serif — it is an ornament rather than text,
                        // and the serif drawing of it is simply a better mark.
                        fontFamily = TIP_FACE.family,
                        textAlign = TextAlign.Center,
                        color = ColorProvider(ink.footer),
                    ),
            )
        }

        // Corner overlay, stacked on top of the content above rather than occupying a row of its
        // own, so it costs the quote+tip block none of the card's vertical space beyond the rail.
        //
        // The outer box is the tap target and is deliberately larger than anything it draws: it
        // sits flush in the card's corner, so the whole corner is within reach, and its padding
        // is what places the visible circle — which means the target grows inwards, over card
        // the tip is not using, instead of pushing the button towards the middle.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd,
        ) {
            Box(
                modifier =
                    GlanceModifier
                        .size(metrics.settingsTapSize)
                        .padding(top = metrics.settingsInset, end = metrics.settingsInset)
                        .clickable(actionStartActivity(Intent(context, SettingsActivity::class.java))),
                contentAlignment = Alignment.TopEnd,
            ) {
                // A shaded, ringed circle behind the icon — rather than a bare glyph — so the
                // button reads as tappable at a glance.
                Box(
                    modifier =
                        GlanceModifier
                            .size(metrics.settingsButtonSize)
                            .cornerRadius(metrics.settingsButtonSize / 2)
                            .background(ImageProvider(ink.settingsButtonRes)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_settings),
                        contentDescription = context.getString(R.string.widget_settings_action),
                        modifier = GlanceModifier.size(metrics.settingsGlyphSize),
                        // The gear vector is a hardcoded white fill (it only ever sat on dark
                        // cards), so it's tinted here rather than duplicated as a second drawable.
                        colorFilter = ColorFilter.tint(ColorProvider(ink.settingsGlyph)),
                    )
                }
            }
        }
    }
}
