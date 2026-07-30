package com.sapglance.app.settings.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sapglance.app.R
import com.sapglance.app.SapGlanceApp
import com.sapglance.core.settings.AppSettings
import com.sapglance.core.settings.SettingsRepository
import com.sapglance.core.settings.VarietyLevel
import com.sapglance.core.tips.DayPart
import com.sapglance.core.tips.Tip
import com.sapglance.core.tips.TipEngine
import com.sapglance.core.tips.TipHistoryRepository
import com.sapglance.core.tips.TipKind
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    tipHistoryRepository: TipHistoryRepository,
    tipEngine: TipEngine,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(AppSettings.DEFAULT) }
    var lastTipText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settingsRepository) {
        settingsRepository.settings.collectLatest { settings = it }
    }

    LaunchedEffect(tipHistoryRepository) {
        tipHistoryRepository.recentTips.collectLatest { recent ->
            lastTipText = recent.lastOrNull()
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
        }

        // "Why this tip?" leads (see the placement discussion in commit history): the user
        // orients on the tip they're actually looking at first, then tunes how tips are picked.
        lastTipText?.let { text ->
            TipSourceSection(
                tipText = text,
                source = tipEngine.findByText(text),
                dayPart = tipEngine.dayPartFor(LocalTime.now()),
                onRefresh = { refreshTipNow(context, settings.varietyLevel) },
            )
        }

        SectionCard {
            VarietySection(
                level = settings.varietyLevel,
                onLevelChange = { level -> scope.launch { settingsRepository.setVarietyLevel(level) } },
            )
        }

        SectionCard {
            AboutSection()
        }
    }
}

/** Which [MaterialTheme] color role a day part's accent is drawn from — kept to the three
 * theme roles (rather than bespoke hardcoded hues) so the hero tip card stays in gamut with
 * dynamic color (Material You) when it's active, not just the static fallback palette. */
private enum class AccentRole { PRIMARY, SECONDARY, TERTIARY }

@Composable
private fun AccentRole.accent(): Color =
    when (this) {
        AccentRole.PRIMARY -> MaterialTheme.colorScheme.primary
        AccentRole.SECONDARY -> MaterialTheme.colorScheme.secondary
        AccentRole.TERTIARY -> MaterialTheme.colorScheme.tertiary
    }

@Composable
private fun AccentRole.onAccent(): Color =
    when (this) {
        AccentRole.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        AccentRole.SECONDARY -> MaterialTheme.colorScheme.onSecondary
        AccentRole.TERTIARY -> MaterialTheme.colorScheme.onTertiary
    }

@Composable
private fun AccentRole.container(): Color =
    when (this) {
        AccentRole.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
        AccentRole.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
        AccentRole.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
    }

private data class DayPartVisual(val icon: ImageVector, val label: String, val role: AccentRole)

/** Sleep's two [DayPart] variants ([DayPart.SLEEP_LATE], [DayPart.SLEEP_EARLY_HOURS]) share one
 * "Night" visual — the split only matters to [TipEngine]'s message selection, not to how this
 * card presents itself. */
@Composable
private fun dayPartVisual(dayPart: DayPart): DayPartVisual =
    when (dayPart) {
        DayPart.MORNING ->
            DayPartVisual(
                Icons.Filled.WbTwilight,
                stringResource(R.string.settings_daypart_morning),
                AccentRole.TERTIARY,
            )
        DayPart.AFTERNOON ->
            DayPartVisual(
                Icons.Filled.WbSunny,
                stringResource(R.string.settings_daypart_afternoon),
                AccentRole.SECONDARY,
            )
        DayPart.EVENING ->
            DayPartVisual(
                Icons.Filled.DarkMode,
                stringResource(R.string.settings_daypart_evening),
                AccentRole.PRIMARY,
            )
        DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS ->
            DayPartVisual(
                Icons.Filled.Bedtime,
                stringResource(R.string.settings_daypart_night),
                AccentRole.PRIMARY,
            )
    }

/** The variety setting is a lean, not a filter (see [TipEngine.messageFor]'s `varietyLevel`
 * parameter): none of the three levels ever remove the practical tips or the
 * philosophical/lighthearted ones entirely, each just shifts which one is the overwhelming
 * majority of what shows up. */
@Composable
private fun VarietySection(
    level: VarietyLevel,
    onLevelChange: (VarietyLevel) -> Unit,
) {
    SectionTitle(icon = Icons.Filled.Tune, text = stringResource(R.string.settings_variety_title))
    AnimatedContent(
        targetState = level,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
        label = "varietyStateDescription",
    ) { animatedLevel ->
        Text(
            text = stringResource(animatedLevel.stateDescriptionRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VarietyLevel.entries.forEach { candidate ->
            VarietyLevelChip(
                level = candidate,
                selected = candidate == level,
                onClick = { onLevelChange(candidate) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Tonal (container-color) fill for the selected level and a plain outline for the other two,
 * rather than a solid `primary`-filled block for the winner — three opaque, equal-weight color
 * blocks side by side read as a stark on/off switch wearing a trenchcoat; a soft container tint
 * plus a per-level icon reads as a considered choice instead. Colors and scale both animate on
 * selection change instead of snapping, so tapping a level reads as a picked choice settling
 * into place rather than a flat state swap. */
@Composable
private fun VarietyLevelChip(
    level: VarietyLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val containerColor by
        animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            label = "varietyChipContainer",
        )
    val borderColor by
        animateColorAsState(
            targetValue = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
            label = "varietyChipBorder",
        )
    val contentColor by
        animateColorAsState(
            targetValue =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            label = "varietyChipContent",
        )
    val scale by
        animateFloatAsState(
            targetValue = if (selected) 1.06f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "varietyChipScale",
        )
    Column(
        modifier =
            modifier
                .scale(scale)
                .clip(shape)
                .background(containerColor)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = level.icon(),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(level.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

private fun VarietyLevel.icon(): ImageVector =
    when (this) {
        VarietyLevel.PRACTICAL -> Icons.Filled.Science
        VarietyLevel.BALANCED -> Icons.Filled.Balance
        VarietyLevel.PLAYFUL -> Icons.Filled.AutoAwesome
    }

/**
 * `null` for [TipKind.PRACTICAL] — the default kind isn't labelled *here*, see [TipSourceSection].
 * The widget does label it (as "Health"), because there the label is the only thing naming the tip;
 * on this card the day part already holds that line.
 */
private fun TipKind.labelRes(): Int? =
    when (this) {
        TipKind.PRACTICAL -> null
        TipKind.MOTIVATION -> R.string.tip_kind_motivation
        TipKind.PHILOSOPHY -> R.string.tip_kind_philosophy
        TipKind.WELLBEING -> R.string.tip_kind_wellbeing
    }

private fun VarietyLevel.labelRes(): Int =
    when (this) {
        VarietyLevel.PRACTICAL -> R.string.settings_variety_label_practical
        VarietyLevel.BALANCED -> R.string.settings_variety_label_balanced
        VarietyLevel.PLAYFUL -> R.string.settings_variety_label_playful
    }

private fun VarietyLevel.stateDescriptionRes(): Int =
    when (this) {
        VarietyLevel.PRACTICAL -> R.string.settings_variety_state_practical
        VarietyLevel.BALANCED -> R.string.settings_variety_state_balanced
        VarietyLevel.PLAYFUL -> R.string.settings_variety_state_playful
    }

/** Picks a new tip out of turn (same selection/anti-repeat logic as the scheduled refresh —
 * see [com.sapglance.core.tips.TipEngine.messageFor]) and pushes it,
 * plus the background style that now follows it, to the widget immediately. Launched in
 * [SapGlanceApp.applicationScope] rather than this screen's own coroutine scope: the
 * latter is cancelled if the user navigates away before the Glance composition finishes,
 * which previously left the widget's Glance session stuck until the app was force-restarted.
 * Goes through [com.sapglance.app.AppContainer.refreshWidget] (not `TipWidget().updateAll()`
 * directly) so this can't race the periodic tick worker or the widget's own tap-to-refresh
 * action and leave a stale render on screen.
 */
private fun refreshTipNow(
    context: Context,
    varietyLevel: VarietyLevel,
) {
    val app = context.applicationContext as SapGlanceApp
    app.applicationScope.launch {
        app.container.advanceTip(LocalTime.now(), varietyLevel = varietyLevel)
        app.container.refreshWidget()
    }
}

/** Shared card chrome for every plain settings section — groups related controls into a clearly
 * bounded, tappable-feeling block instead of a flat list separated by thin dividers. The hero
 * tip card ([TipSourceSection]) draws its own chrome instead of using this, since its whole
 * point is to not look like a plain list row. */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

/**
 * [source] is a best-effort lookup ([TipEngine.findByText] matching the persisted plain-text
 * history against the live catalog) and can be null — e.g. right after a wording edit to the
 * tip catalog, the last tip a user was shown may no longer match any current entry
 * byte-for-byte. The card (and the refresh button) must still show in that case: only the
 * citation half depends on a successful match, not the card's existence.
 *
 * Tinted by [dayPart] rather than a fixed color: the card is meant to read as "the tip that's on
 * your home screen right now", so its accent follows the same signal ([TipEngine.dayPartFor])
 * that picked that tip in the first place.
 */
@Composable
private fun TipSourceSection(
    tipText: String,
    source: Tip?,
    dayPart: DayPart,
    onRefresh: () -> Unit,
) {
    val visual = dayPartVisual(dayPart)
    val accent = visual.role.accent()
    val scope = rememberCoroutineScope()
    val refreshIconRotation = remember { Animatable(0f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(visual.role.container(), MaterialTheme.colorScheme.surfaceContainer),
                        ),
                    )
                    .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.role.onAccent(),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Tone tips name their group next to the day part ("EVENING · PHILOSOPHY"), so
                // it's obvious which pool a tip came from. Practical tips stay unlabelled: they
                // are the default and the majority, and tagging every one of them would be noise.
                val kindLabelRes = source?.kind?.labelRes()
                Text(
                    text =
                        if (kindLabelRes == null) {
                            visual.label.uppercase()
                        } else {
                            stringResource(
                                R.string.settings_tip_label_combined,
                                visual.label,
                                stringResource(kindLabelRes),
                            ).uppercase()
                        },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = accent,
                )
            }
            Spacer(Modifier.height(16.dp))
            // Slides the incoming tip up past the outgoing one rather than a hard cut, so a
            // refresh reads as one tip replacing another instead of the text just changing.
            AnimatedContent(
                targetState = tipText,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { height -> height / 4 })
                        .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(150)) { height -> -height / 4 })
                },
                label = "tipQuote",
            ) { animatedTipText ->
                Text(
                    text = stringResource(R.string.settings_tip_source_quote, animatedTipText),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            // An unsourced tip (an original motivational or wellbeing line) shows no citation
            // block at all rather than an empty header — it makes no empirical claim, so there
            // is nothing to back up and nothing to apologise for.
            if (source != null && source.sources.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                val context = LocalContext.current
                // Every source gets its own tappable row rather than just the first one: the
                // point of carrying several is that the user can see a claim is backed by more
                // than one study, which a single collapsed link would hide again.
                Text(
                    text =
                        if (source.kind.requiresCitation) {
                            pluralStringResource(
                                R.plurals.settings_tip_sources_header,
                                source.sources.size,
                                source.sources.size,
                            )
                        } else {
                            stringResource(R.string.settings_tip_source_attribution)
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                source.sources.forEach { tipSource ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.clickable { openSource(context, tipSource.url) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tipSource.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {
                    // A full spin per tap, purely decorative (independent of onRefresh's own
                    // async work) — it's a tactile "something happened" cue that fires the
                    // instant you tap, rather than waiting on the actual tip/widget update.
                    scope.launch {
                        refreshIconRotation.animateTo(
                            targetValue = refreshIconRotation.value + 360f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        )
                    }
                    onRefresh()
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).rotate(refreshIconRotation.value),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.settings_tip_refresh_action))
            }
        }
    }
}

/** Hands off to the system browser; no INTERNET permission needed since the browser process,
 * not this app, makes the request — see the "100% offline" note atop AndroidManifest.xml. */
private fun openSource(
    context: Context,
    url: String,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser available on this device — nothing sensible to do, so skip silently.
    }
}

/** Collapsed by default: this is boilerplate/legal-ish content the user rarely needs to
 * revisit, so it shouldn't cost permanent scroll space on every settings visit. */
@Composable
private fun AboutSection() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "aboutChevron")

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.settings_about_title), style = MaterialTheme.typography.titleLarge)
        }
        // One icon rotated 180° rather than swapping between ExpandMore/ExpandLess drawables —
        // the chevron turns smoothly in place instead of one glyph replacing another.
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.rotate(chevronRotation),
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            Spacer(Modifier.height(12.dp))
            AboutRow(icon = Icons.Filled.PhoneAndroid, text = stringResource(R.string.settings_about_privacy_local))
            Spacer(Modifier.height(8.dp))
            AboutRow(icon = Icons.Filled.PersonOff, text = stringResource(R.string.settings_about_privacy_account))
            Spacer(Modifier.height(8.dp))
            AboutRow(icon = Icons.Filled.VisibilityOff, text = stringResource(R.string.settings_about_privacy_ads))
        }
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    text: String,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.titleLarge)
        }
        trailing()
    }
    Spacer(Modifier.height(8.dp))
}
