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
import com.sapglance.core.widget.WidgetStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import kotlin.math.ceil

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

            GlanceTheme {
                TipWidgetContent(tip, WidgetStyle.forTip(tip))
            }
        }
    }
}

/**
 * Every color the card draws text and chrome in, for one direction of contrast.
 *
 * The widget used to hardcode white text with a translucent *black* chip behind it, which
 * silently assumed every background would stay dark — and did, for as long as they all were.
 * The moment a pale style exists that assumption produces white-on-cream: technically rendered,
 * practically invisible. So the whole ink set flips together rather than the text color alone;
 * a light card needs a *lighter* chip than its background (not a darker one), a dark card
 * frame instead of a white one, and a dark gear glyph on a bright button instead of the
 * reverse. Flipping only some of those is what leaves a card looking half-inverted.
 */
private enum class WidgetInk(
    val text: Color,
    val chip: Color,
    val quoteMark: Color,
    val footer: Color,
    val settingsButtonRes: Int,
    val settingsGlyph: Color,
) {
    ON_DARK(
        text = Color.White,
        // 0.22 was doing too little to be worth drawing: on the busier styles the panel read as
        // a smudge rather than a surface, so the text still took its contrast from whatever art
        // happened to be behind it. Deeper enough to actually sit the words on something, and
        // still translucent enough that the artwork reads through it.
        //
        // Deepened again for the monospaced face, which is drawn light and has no bold weight
        // available (see [TIP_FACE]). Contrast is one of the only two things that can still be
        // spent on making thin strokes read as solid; the other is size.
        chip = Color.Black.copy(alpha = 0.44f),
        quoteMark = Color.White.copy(alpha = 0.6f),
        footer = Color.White.copy(alpha = 0.8f),
        settingsButtonRes = R.drawable.widget_settings_button_bg,
        settingsGlyph = Color.White.copy(alpha = 0.9f),
    ),

    // Not pure black: against a bright card, #000 has a hard glare-y edge that a near-black
    // with a trace of the background's own warmth doesn't. The chip goes white and *up* in
    // alpha compared to ON_DARK's, because on a light style its job changes — it isn't
    // darkening a scene behind pale text, it's flattening whatever art the dark text crosses.
    ON_LIGHT(
        text = Color(0xFF17181C),
        chip = Color.White.copy(alpha = 0.72f),
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
 * The longest tip the catalog allows. The pools are capped at roughly this, and the cap is a
 * content rule rather than something enforced in code — see `tips/general.txt`. Sizing against
 * the worst case is what keeps this from being a per-tip measurement: every tip renders at the
 * same size on a given card, so nothing shifts underneath the reader when the tip changes.
 */
private const val LONGEST_TIP_CHARS = 90

/** The usual line box as a multiple of font size. */
private const val LINE_HEIGHT_RATIO = 1.25f

/**
 * A typeface together with the character-width figure measured for *that* face.
 *
 * These are one type rather than two constants because they are one decision. They were separate
 * once, and the predictable happened: the face changed from serif to condensed and the width
 * figure — measured against the serif — stayed behind. Nothing broke loudly, since a stale figure
 * over-reserves rather than clipping, but the card silently sized its type for a font it was no
 * longer using and gave back a third of its own height. Pairing them means changing the face
 * without re-measuring is not something you can forget to do; it is something you cannot express.
 *
 * [effectiveCharWidthRatio] is the effective width of one character as a fraction of font size,
 * **measured from a real render, never derived from the font's metrics.** It deliberately folds
 * two things together — glyph advance and word-wrap waste — because only their product affects
 * the line count, and only their product can be counted off a screenshot. A line breaks at the
 * last word that fits, so every line but the last ends in dead space, and the narrower the column
 * the larger that share.
 *
 * To re-measure: screenshot the widget, count the rendered lines of a tip of known length, and
 * take `textWidth / (fontSize * charsPerLine)` where `charsPerLine` is the tip's length divided by
 * the line count. Round *up* — over-estimating costs a little space, under-estimating clips.
 */
private data class TipFace(
    val family: FontFamily,
    val effectiveCharWidthRatio: Float,
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
 * times a proportional font's (0.605 em against this face's 0.260) and the text reads as though
 * it has been pulled apart. That is not tunable; it is what monospace *is*.
 *
 * It also cost size twice over. Every glyph takes the width of the widest, so fewer characters
 * fit per line, so more lines are needed for the same tip — and under a line cap (see
 * [MAX_TIP_LINES]) that shortfall is paid back in font size: 14sp where this face gets 16sp and
 * Source Sans Pro would get 18sp.
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
        // Derived from the font file rather than eyeballed. NotoSerif-Bold.ttf's `hmtx` table
        // averages 0.5010 em over real tip text (space 0.260, 'm' 0.986, 'i' 0.352 — proportional,
        // unlike the monospaced face this replaced, where all three measured 0.605).
        //
        // Advance is only half of it; the rest is wrap waste. Checked against a real render: an
        // 88-character tip — the catalog's worst case is 90 — set in 6 lines at 16sp on the
        // 187x226dp card, so ~14.7 characters a line and an effective 0.67. Wrap waste is
        // therefore running about 1.33x for this face, rather than the 1.13x measured for Source
        // Sans Pro; a wider face fits fewer characters per line and forfeits proportionally more
        // to the part-word at the end.
        //
        // Shipped at 0.62, which is *below* that measurement rather than above it, and that is
        // safe only because of how [MAX_TIP_LINES] now works. It biases the ladder one rung larger
        // (16sp rather than 15sp) and means the very longest tips may take a seventh line instead
        // of six — which the rendered `maxLines` allows, since it follows the card's real height.
        // Under the old hard cap this exact under-estimate is what truncated tips. Read the two
        // together: the ratio may be optimistic about *line count*, never about *height*.
        effectiveCharWidthRatio = 0.62f,
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
 * at a given widget size — nothing can drift out of sync with reality. Real text wrapping still
 * decides the line count; this only decides how much room wrapping gets to happen in.
 *
 * The decorative chrome is what gives way first as the card shrinks, because it is the only part
 * that isn't the point. Chrome is charged before the type is sized, so every dp of it is a dp the
 * ladder cannot spend — and the arithmetic is unsentimental about which dp those are. On the
 * default 2x2 (~154x183dp) the layout used to spend 45% of the card's height on padding, a quote
 * glyph and a footer gap before drawing a single word, which left 11sp type: the shape the phrase
 * "small text on an empty card" actually describes. Trimming the doubled-up padding and holding
 * the `❝` glyph back for cards tall enough to afford it brings that to 24% and the type to 13sp.
 *
 * The app-name footer is charged at every size and never dropped — a card with no name on it
 * reads as unfinished rather than minimal, and the label is cheap next to the glyph.
 */
private data class CardMetrics(
    val tipFontSize: TextUnit,
    val maxTipLines: Int,
    val showQuoteMark: Boolean,
    val quoteMarkSize: TextUnit,
    val footerFontSize: TextUnit,
    val footerSpacing: Dp,
    val cardPadding: Dp,
    val chipPaddingVertical: Dp,
    val chipPaddingHorizontal: Dp,
    val chipCornerRadius: Dp,
    val settingsButtonSize: Dp,
    val settingsGlyphSize: Dp,
)

/**
 * Derived from the size rather than matched against the buckets above, so any size the host hands
 * back lands somewhere sensible instead of falling into a `when` branch that had to guess.
 *
 * The two axes are treated separately because they constrain different things, and conflating
 * them is what made the first version of this too rigid:
 *
 * - **Width sets the font size**, because width is what decides characters per line. A narrow
 *   card at 15sp fits about twelve characters before wrapping, which turns a 90-character tip
 *   into eight lines no card that narrow can show.
 * - **Height sets how much decoration survives**, because the `❝` glyph and the app-name footer
 *   cost fixed vertical space that a short card needs for the tip itself.
 *
 * `maxTipLines` then falls out of the arithmetic rather than being another hand-tuned constant:
 * whatever height is left after chrome, divided by the line height the font implies. Note this is
 * still not measuring the *text* — it never asks how a particular tip will wrap, which is the
 * prediction that failed before. It only asks how many lines of any text this card can display.
 */
private fun metricsFor(size: DpSize): CardMetrics {
    // Short cards give up the quote glyph, but never the name: an unbranded card reads as an
    // empty one. The footer instead shrinks with the card, which costs far less height than the
    // glyph does and keeps the widget identifiable at every size.
    val compact = size.height < 140.dp

    // The glyph has to earn its height, and the bar has risen twice. It began at 170dp, where the
    // default 2x2 (~154x183dp) drew it and the ~26dp it costs — 14% of that card — pushed the tip
    // two rungs down the ladder. That is the hollow failure exactly: the ornament ends up the
    // largest thing on the card and the sentence the smallest.
    //
    // 200dp fixed that card and not the next one up. On a real 187x226dp 2x2 the glyph was still
    // costing 23dp, which is the difference between 16sp and 18sp — and with a monospaced face
    // that cannot be made any heavier (see [TIP_FACE]), size is one of only two levers left
    // against type that reads as thin. An ornament is not worth two points of type on the card
    // most people will actually have. Past 250dp there is genuinely room for both.
    val showQuoteMark = size.height >= 250.dp
    val quoteMarkSize = if (size.height >= 290.dp) 24.sp else 20.sp
    val footerFontSize =
        when {
            compact -> 8.sp
            size.height < 210.dp -> 10.sp
            else -> 11.sp
        }
    // The footer read as adrift because of the gap above it, not its size, so the gap is what
    // shrinks here. Scaling the label down instead would have made the card look emptier, not
    // tighter.
    val footerSpacing = if (compact) 2.dp else 4.dp
    // Horizontal padding gets charged twice — once by the card, once by the chip inside it — so
    // the old 12+8 spent 40dp of a 154dp card, a full quarter of its width, on margin alone.
    // Every dp handed back widens the text column, and the column width is what sets characters
    // per line and so the largest font the ladder can afford. A 2x2 card is tight enough that
    // this is the difference between fitting the catalog's longest tip and truncating it, which
    // is why the compact figures are meaner still.
    val padding =
        when {
            compact -> 4.dp
            size.height < 240.dp -> 8.dp
            else -> 10.dp
        }
    val chipPadding = if (compact) 3.dp else 6.dp
    val chipHorizontalPadding = if (compact) 5.dp else 7.dp
    // Rounder than the old flat 14dp, and scaled so the panel keeps its proportions instead of
    // looking progressively boxier as the card grows. Nested inside the card's own 20dp corner,
    // a softer radius reads as one shape sitting inside another rather than as a rectangle
    // pasted over artwork — which, with the fuller-width panel, is the shape most visible now.
    val chipCornerRadius =
        when {
            compact -> 12.dp
            size.height < 240.dp -> 18.dp
            else -> 22.dp
        }
    // The gear scales with the card for the same reason the type does. A fixed 36dp circle is
    // 23% of a 154dp card's width, which reads as a button with a card attached rather than a
    // card with a button on it.
    val settingsButtonSize =
        when {
            compact -> 26.dp
            size.height < 200.dp -> 30.dp
            else -> 34.dp
        }

    // Chrome first, because none of it depends on the tip's font size — so what's left is a
    // fixed budget the type has to fit inside. Every ratio here is an estimate, and each one
    // errs towards a smaller font rather than a clipped tip.
    val quoteMarkHeight =
        if (showQuoteMark) (quoteMarkSize.value * LINE_HEIGHT_RATIO).dp + 2.dp else 0.dp
    val footerHeight = (footerFontSize.value * LINE_HEIGHT_RATIO).dp + footerSpacing
    val chromeHeight = padding * 2 + chipPadding * 2 + quoteMarkHeight + footerHeight
    val availableHeight = (size.height - chromeHeight).value
    val textWidth = (size.width - padding * 2 - chipHorizontalPadding * 2).value

    // Largest font whose worst case still fits, rather than a hand-drawn map from width ranges
    // to font sizes. That map had no way to notice spare *height*, so a narrow-but-tall card —
    // the shape a 2-column phone slot actually produces — got type sized for a short card and
    // looked half empty. This asks the question that matters instead: how big can the type be
    // before the longest tip in the catalog stops fitting?
    val fontSize =
        TIP_FONT_LADDER.lastOrNull { candidate ->
            val charsPerLine = textWidth / (candidate.value * TIP_FACE.effectiveCharWidthRatio)
            if (charsPerLine < 1f) {
                false
            } else {
                val linesNeeded = ceil(LONGEST_TIP_CHARS / charsPerLine)
                // Two conditions, and the line count is the *stricter* of them on a tall card:
                // height alone would happily allow eight or nine lines of small type, which is
                // a paragraph rather than a glance. See [MAX_TIP_LINES].
                linesNeeded <= MAX_TIP_LINES &&
                    linesNeeded * candidate.value * LINE_HEIGHT_RATIO <= availableHeight
            }
        } ?: TIP_FONT_LADDER.first()

    val usableLines = (availableHeight / (fontSize.value * LINE_HEIGHT_RATIO)).toInt()

    return CardMetrics(
        tipFontSize = fontSize,
        // Note this is *not* clamped to [MAX_TIP_LINES]. That ceiling shapes which font gets
        // picked; it must never be what a tip is cut off at. See [MAX_TIP_LINES].
        maxTipLines = usableLines.coerceAtLeast(MIN_TIP_LINES),
        showQuoteMark = showQuoteMark,
        quoteMarkSize = quoteMarkSize,
        footerFontSize = footerFontSize,
        footerSpacing = footerSpacing,
        cardPadding = padding,
        chipPaddingVertical = chipPadding,
        chipPaddingHorizontal = chipHorizontalPadding,
        chipCornerRadius = chipCornerRadius,
        settingsButtonSize = settingsButtonSize,
        settingsGlyphSize = settingsButtonSize * 0.55f,
    )
}

/** Floor so a tiny card still shows a couple of lines. */
private const val MIN_TIP_LINES = 2

/**
 * How many lines a tip should *aim* to wrap to. Six, because past that a glance turns into a
 * paragraph — the card stops being read and starts being skimmed.
 *
 * **A target for choosing the font, never a limit on what gets drawn.** This distinction has now
 * been got wrong twice, in both directions, so it is worth stating plainly.
 *
 * The first version was a flat `maxLines` applied after the size had been chosen. It cut the ends
 * off tips at roughly 1,700 sizes across the declared range, and was removed.
 *
 * The second version constrained font selection — correctly — but *also* clamped the rendered
 * `maxLines` to the same number, and that quietly reintroduced the clipping. The selection can
 * only reason about an average tip, because [effectiveCharWidthRatio] folds in an *average* wrap
 * waste; a particular tip with long words wraps worse than average and needs a seventh line. The
 * font was sized so six lines usually suffice, and then the seventh was chopped off.
 *
 * So the two numbers are now deliberately different. Font selection targets this ceiling; the
 * rendered `maxLines` is whatever the card's height genuinely allows, which is always more. Tips
 * come out at six lines or fewer nearly always, occasionally seven, and never truncated — because
 * a line count is a matter of taste and a cut-off sentence is a defect.
 *
 * [TIP_FONT_LADDER] runs down to 6sp so the target stays reachable: a 110dp-wide card cannot fit
 * 90 characters into six lines at any larger size.
 */
private const val MAX_TIP_LINES = 6

@Composable
private fun TipWidgetContent(
    tip: String,
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
    val metrics = metricsFor(LocalSize.current)

    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ImageProvider(backgroundRes))
                // Whole-card tap: refreshes the tip in place. The settings icon below has its
                // own clickable modifier, which takes the tap over this one within its bounds.
                .clickable(actionRunCallback<RefreshTipAction>())
                .padding(metrics.cardPadding),
        contentAlignment = Alignment.Center,
    ) {
        // Centred in the *whole* card, not in what is left after the footer.
        //
        // This used to be a Column of [weighted box | spacer | footer], which centred the tip
        // inside the card minus the footer strip and so sat it about 9dp high — subtle in
        // isolation, obvious once you look for it, and it made the card read as top-heavy. The
        // footer is a corner overlay now (below), exactly like the settings button, so this box
        // gets the full height and its centre is the card's centre.
        //
        // Nothing collides, and the margin is arithmetic rather than luck: `availableHeight` in
        // [metricsFor] already deducts the footer, so the tallest possible block is
        // `card - 2*cardPadding - 2*chipPadding - footer`. Centring that leaves
        // `cardPadding + chipPadding` of clearance below it, which is comfortably more than the
        // footer strip it must clear.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Dropped outright on a compact card. It is the single most expensive piece
                    // of pure decoration in the layout (~22dp of glyph plus its spacer), and a
                    // tip nobody can read costs more than a missing flourish.
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
                        Spacer(GlanceModifier.height(2.dp))
                    }
                    // A translucent "chip" behind the tip, rather than bare text over the
                    // gradient/art: guarantees contrast no matter where on the gradient (or
                    // over which piece of background art) the text lands, and gives the tip
                    // its own visible frame instead of floating loose over the artwork. Which
                    // way it pushes the local background — darker or lighter — comes from the
                    // ink, since "add contrast" means opposite things on the two card families.
                    //
                    // It spans the full width rather than hugging the text. Wrapping made the
                    // chip's own width depend on how the longest line happened to break, so a
                    // short tip drew a small pill adrift in the middle of the card and the
                    // frame moved every time the tip changed. Full width makes it a panel: a
                    // fixed, deliberate-looking block the text sits inside, and one that stays
                    // put across refreshes. It costs the tip no room — the text already got
                    // this width whenever it wrapped at all.
                    Box(
                        modifier =
                            GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(ink.chip))
                                .cornerRadius(metrics.chipCornerRadius)
                                .padding(
                                    horizontal = metrics.chipPaddingHorizontal,
                                    vertical = metrics.chipPaddingVertical,
                                ),
                    ) {
                        Text(
                            text = tip,
                            maxLines = metrics.maxTipLines,
                            // Must fill the chip, now that the chip is wider than the text.
                            // `textAlign` centres lines within the *TextView's own* measured
                            // width, and a wrap-content TextView inside the Box (a FrameLayout,
                            // gravity start) is exactly as wide as its longest line — so a tip
                            // short enough not to wrap would centre inside itself and then sit
                            // flush against the panel's left edge, looking un-centred for the
                            // one case where the centring is most obvious. Filling the width
                            // makes the two agree.
                            modifier = GlanceModifier.fillMaxWidth(),
                            style =
                                TextStyle(
                                    fontSize = metrics.tipFontSize,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = TIP_FACE.family,
                                    textAlign = TextAlign.Center,
                                    color = ColorProvider(ink.text),
                                ),
                        )
                    }
                }
            }
        }

        // Shown at every size. An earlier version dropped this on small cards to buy height,
        // which made the widget look unfinished rather than minimal — a card with no name on it
        // reads as empty. Scaling the label down costs a few dp where hiding it saved about
        // twenty, and that difference is affordable.
        //
        // An overlay rather than the last row of a Column: in the flow it pushed the tip off the
        // card's true centre by half its own height. Its height is still *charged* against the
        // tip's budget in [metricsFor] — it just no longer decides where the tip sits.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
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

        // Corner overlay, stacked on top of the content above rather than occupying a row of
        // its own, so it costs the quote+tip block none of the card's vertical space.
        //
        // Bottom corner, not the top one it used to sit in. An overlay this size always covers
        // some of the panel behind it, so the question is only *which* line it lands on, and the
        // two corners answer that very differently. The tip is centre-aligned, so its first line
        // runs the full width of the panel whenever the tip is long — a top-end button therefore
        // sat squarely on real words, and freeing the top for full-size type (see the quote-mark
        // threshold in [metricsFor]) would have pushed the text further under it. The last line of
        // centre-aligned wrapped text is the short one, and it is centred, so the bottom-end
        // corner is the part of the panel most reliably empty. It also lands mostly over the
        // footer strip, where a centred one-word label leaves the ends genuinely unused — so the
        // same move that stops the button covering text also stops that strip looking bare.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            // A shaded, ringed circle behind the icon — rather than a bare glyph — so the
            // button reads as tappable at a glance, and so the whole circle (not just the
            // glyph inside it) is the actual tap target.
            Box(
                modifier =
                    GlanceModifier
                        .size(metrics.settingsButtonSize)
                        .cornerRadius(metrics.settingsButtonSize / 2)
                        .background(ImageProvider(ink.settingsButtonRes))
                        .clickable(actionStartActivity(Intent(context, SettingsActivity::class.java))),
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
