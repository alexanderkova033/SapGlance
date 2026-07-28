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
        chip = Color.Black.copy(alpha = 0.22f),
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
        chip = Color.White.copy(alpha = 0.5f),
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
 */
private val TIP_FONT_LADDER = listOf(10.sp, 11.sp, 13.sp, 15.sp, 18.sp, 21.sp)

/**
 * The longest tip the catalog allows. The pools are capped at roughly this, and the cap is a
 * content rule rather than something enforced in code — see `tips/general.txt`. Sizing against
 * the worst case is what keeps this from being a per-tip measurement: every tip renders at the
 * same size on a given card, so nothing shifts underneath the reader when the tip changes.
 */
private const val LONGEST_TIP_CHARS = 90

/** Rough advance width of the bold serif face as a fraction of font size, and the usual line box. */
private const val CHAR_WIDTH_RATIO = 0.5f
private const val LINE_HEIGHT_RATIO = 1.25f

/** Horizontal padding inside the chip, both sides, which the text never gets to use. */
private val CHIP_HORIZONTAL_PADDING = 8.dp

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
 * that isn't the point: at [COMPACT_CARD] the `❝` glyph and the app-name footer are dropped
 * entirely, which buys back ~64dp of the ~92dp the full layout spends before drawing any tip.
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
    val showQuoteMark = size.height >= 170.dp
    val quoteMarkSize = if (size.height >= 230.dp) 24.sp else 18.sp
    val footerFontSize =
        when {
            compact -> 8.sp
            size.height < 210.dp -> 10.sp
            else -> 11.sp
        }
    val footerSpacing = if (compact) 3.dp else 8.dp
    val padding = if (compact) 6.dp else 12.dp
    val chipPadding = if (compact) 4.dp else 6.dp

    // Chrome first, because none of it depends on the tip's font size — so what's left is a
    // fixed budget the type has to fit inside. Every ratio here is an estimate, and each one
    // errs towards a smaller font rather than a clipped tip.
    val quoteMarkHeight =
        if (showQuoteMark) (quoteMarkSize.value * LINE_HEIGHT_RATIO).dp + 4.dp else 0.dp
    val footerHeight = (footerFontSize.value * LINE_HEIGHT_RATIO).dp + footerSpacing
    val chromeHeight = padding * 2 + chipPadding * 2 + quoteMarkHeight + footerHeight
    val availableHeight = (size.height - chromeHeight).value
    val textWidth = (size.width - padding * 2 - CHIP_HORIZONTAL_PADDING * 2).value

    // Largest font whose worst case still fits, rather than a hand-drawn map from width ranges
    // to font sizes. That map had no way to notice spare *height*, so a narrow-but-tall card —
    // the shape a 2-column phone slot actually produces — got type sized for a short card and
    // looked half empty. This asks the question that matters instead: how big can the type be
    // before the longest tip in the catalog stops fitting?
    val fontSize =
        TIP_FONT_LADDER.lastOrNull { candidate ->
            val charsPerLine = textWidth / (candidate.value * CHAR_WIDTH_RATIO)
            if (charsPerLine < 1f) {
                false
            } else {
                val linesNeeded = ceil(LONGEST_TIP_CHARS / charsPerLine)
                linesNeeded * candidate.value * LINE_HEIGHT_RATIO <= availableHeight
            }
        } ?: TIP_FONT_LADDER.first()

    val usableLines = (availableHeight / (fontSize.value * LINE_HEIGHT_RATIO)).toInt()

    return CardMetrics(
        tipFontSize = fontSize,
        maxTipLines = usableLines.coerceIn(MIN_TIP_LINES, MAX_TIP_LINES),
        showQuoteMark = showQuoteMark,
        quoteMarkSize = quoteMarkSize,
        footerFontSize = footerFontSize,
        footerSpacing = footerSpacing,
        cardPadding = padding,
        chipPaddingVertical = chipPadding,
    )
}

/** Floor so a tiny card still shows a couple of lines; ceiling so a huge one doesn't sprawl. */
private const val MIN_TIP_LINES = 2
private const val MAX_TIP_LINES = 8

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
        // The settings button used to sit in its own header Row above this Column, which
        // reserved a full-width strip purely for a 36dp circle and squeezed the quote+tip
        // block into whatever height was left. It's now a corner overlay (below) instead, so
        // this Column — and the quote+tip inside it — gets the card's entire height.
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
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
                        Spacer(GlanceModifier.height(4.dp))
                    }
                    // A translucent "chip" behind the tip, rather than bare text over the
                    // gradient/art: guarantees contrast no matter where on the gradient (or
                    // over which piece of background art) the text lands, and gives the tip
                    // its own visible frame instead of floating loose over the artwork. Which
                    // way it pushes the local background — darker or lighter — comes from the
                    // ink, since "add contrast" means opposite things on the two card families.
                    Box(
                        modifier =
                            GlanceModifier
                                .background(ColorProvider(ink.chip))
                                .cornerRadius(14.dp)
                                .padding(horizontal = 8.dp, vertical = metrics.chipPaddingVertical),
                    ) {
                        Text(
                            text = tip,
                            maxLines = metrics.maxTipLines,
                            style =
                                TextStyle(
                                    fontSize = metrics.tipFontSize,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    textAlign = TextAlign.Center,
                                    color = ColorProvider(ink.text),
                                ),
                        )
                    }
                }
            }
            // Shown at every size. An earlier version dropped this on small cards to buy height,
            // which made the widget look unfinished rather than minimal — a card with no name on
            // it reads as empty. Scaling the label down costs a few dp where hiding it saved
            // about twenty, and that difference is affordable.
            Spacer(GlanceModifier.height(metrics.footerSpacing))
            Text(
                // Derived from the centralized app_name resource (rather than a second hardcoded
                // string) so changing the product name only ever means editing strings.xml.
                text = context.getString(R.string.app_name).uppercase(),
                style =
                    TextStyle(
                        fontSize = metrics.footerFontSize,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = ColorProvider(ink.footer),
                    ),
            )
        }

        // Corner overlay, stacked on top of the content above rather than occupying a row of
        // its own: same visual spot as before (inset by the root Box's own 12dp/16dp padding),
        // but it no longer costs the quote+tip block any of the card's vertical space.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd,
        ) {
            // A shaded, ringed circle behind the icon — rather than a bare glyph — so the
            // button reads as tappable at a glance, and so the whole 36dp circle (not just
            // the 20dp glyph inside it) is the actual tap target.
            Box(
                modifier =
                    GlanceModifier
                        .size(36.dp)
                        .cornerRadius(18.dp)
                        .background(ImageProvider(ink.settingsButtonRes))
                        .clickable(actionStartActivity(Intent(context, SettingsActivity::class.java))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_settings),
                    contentDescription = context.getString(R.string.widget_settings_action),
                    modifier = GlanceModifier.size(20.dp),
                    // The gear vector is a hardcoded white fill (it only ever sat on dark
                    // cards), so it's tinted here rather than duplicated as a second drawable.
                    colorFilter = ColorFilter.tint(ColorProvider(ink.settingsGlyph)),
                )
            }
        }
    }
}
