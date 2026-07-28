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

/**
 * FR1: shows the current tip. Tapping the card itself gets a new tip on the spot (via
 * [RefreshTipAction], without opening the app); the small gear icon in the corner is the
 * only way into the settings screen — AppWidgets can't intercept long-press, the launcher
 * reserves that gesture for its own move/resize/remove UI, so a dedicated tap target is the
 * only reliable option. [WidgetRefreshWorker] is what normally advances the tip on a timer;
 * this only computes a fallback tip itself on the very first render (e.g. right after install,
 * before any worker has run yet), reusing the same persisted "last tip" that notifications also
 * read/write so the anti-repeat guarantee (FR5) holds across both surfaces. Whichever trigger
 * advances the tip, the widget repaints by *observing* the persisted history rather than by
 * being handed a value — see the comment inside [provideGlance] for why that distinction is
 * what makes the repaint happen at all.
 */
class TipWidget : GlanceAppWidget() {
    // Responsive rather than Single: the card has to survive being resized, and its chrome
    // costs roughly 90dp of height before a line of tip is drawn. At the small end of the
    // range the widget used to advertise, that left the tip *negative* space and it clipped —
    // the confirmed defect this addresses. Glance composes once per declared size and hands
    // the host a RemoteViews per bucket, so the launcher picks the right one without the app
    // re-rendering on every resize.
    override val sizeMode = SizeMode.Responsive(setOf(COMPACT_CARD, MEDIUM_CARD, LARGE_CARD))

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

// The three shapes the card is composed for. Glance picks the largest that fits the space the
// launcher actually gave the widget, so these are buckets to design against, not sizes anyone
// is forced into.
private val COMPACT_CARD = DpSize(180.dp, 110.dp)
private val MEDIUM_CARD = DpSize(250.dp, 180.dp)
private val LARGE_CARD = DpSize(320.dp, 250.dp)

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
    val showFooter: Boolean,
    val cardPadding: Dp,
    val cardPaddingVertical: Dp,
    val chipPaddingVertical: Dp,
)

// Thresholds on height rather than equality against the DpSizes above: the host can hand back a
// size that isn't exactly one of the declared buckets, and a `when` on ranges degrades sensibly
// where an exhaustive match on exact values would have to guess.
private fun metricsFor(size: DpSize): CardMetrics =
    when {
        size.height < 140.dp ->
            CardMetrics(
                tipFontSize = 12.sp,
                maxTipLines = 5,
                showQuoteMark = false,
                showFooter = false,
                cardPadding = 10.dp,
                cardPaddingVertical = 10.dp,
                chipPaddingVertical = 4.dp,
            )

        size.height < 210.dp ->
            CardMetrics(
                tipFontSize = 14.sp,
                maxTipLines = 6,
                showQuoteMark = true,
                showFooter = true,
                cardPadding = 14.dp,
                cardPaddingVertical = 14.dp,
                chipPaddingVertical = 5.dp,
            )

        else ->
            CardMetrics(
                tipFontSize = 15.sp,
                maxTipLines = 6,
                showQuoteMark = true,
                showFooter = true,
                cardPadding = 12.dp,
                cardPaddingVertical = 16.dp,
                chipPaddingVertical = 6.dp,
            )
    }

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
                .padding(horizontal = metrics.cardPadding, vertical = metrics.cardPaddingVertical),
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
                                    fontSize = 18.sp,
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
            // Branding is the other thing that yields on a compact card, for the same reason as
            // the quote glyph: the tip is what the widget is for. The name still reaches the
            // user through the launcher, the settings screen and the widget picker.
            if (metrics.showFooter) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    // Derived from the centralized app_name resource (rather than a second
                    // hardcoded string) so changing the final product name only ever means
                    // editing strings.xml in one place.
                    text = context.getString(R.string.app_name).uppercase(),
                    style =
                        TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = ColorProvider(ink.footer),
                        ),
                )
            }
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
