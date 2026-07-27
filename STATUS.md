# Project status

_Last updated: 2026-07-27_

This file is a snapshot for picking the project back up — it will go stale; check `git log`
and the repo itself for anything that matters more than this doc.

## What's built

All v1 milestones are implemented and on `main` (pushed to
[github.com/alexanderkova033/health-widget](https://github.com/alexanderkova033/health-widget)):

1. `chore:` Gradle Kotlin DSL skeleton, version catalog, CI, ktlint
2. `feat:` `:core` module — `TipEngine` + full unit tests
3. `feat:` DataStore-backed settings + last-tip persistence
4. `feat:` WorkManager nudge + sleep-alert notifications, quiet hours
5. `feat:` Glance home-screen widget, refresh scheduling, boot rescheduling
6. `feat:` Compose settings screen wired to DataStore
7. `docs:` README, PRIVACY.md, LICENSE, CONTRIBUTING.md

Two follow-up passes after the initial build-out:

- **`fix:` correct Glance API usage and ktlint/lint violations found by a real build** — set
  up a JDK + scratch Android SDK to actually compile/test/lint instead of relying on manual
  review, and fixed real bugs that review alone had missed (wrong `GlanceTheme` import,
  missing `updateAll` import, wrong `actionStartActivity` signature, a lint
  `MissingPermission` false-negative from a wrapper function).
- **`refactor:` reorganize into screaming + clean architecture** — `:core` split into
  `tips/`, `settings/`, `scheduling/` domain packages with repository *interfaces*
  (`SettingsRepository`, `TipHistoryRepository`) and a new `AdvanceTipUseCase` that
  de-duplicated tip-picking logic that had been hand-copied across three call sites.
  `:app` reorganized by feature (`settings/`, `tips/`, `notifications/`, `widget/`, `boot/`)
  instead of by technical layer; `MainActivity` renamed `SettingsActivity`.
- **`docs:` scientifically ground and expand tip content (2026-07-24)** — reviewed every
  existing tip against primary research and tightened the handful that overreached or were
  imprecise (e.g. the afternoon "post-lunch dip" now correctly framed as partly circadian
  rather than just food; the cold-water-splash tip now names the actual dive-reflex
  mechanism; the alcohol tip now notes REM suppression instead of just "reduced quality").
  Added ~33 new research-grounded tips (`general.txt` 30→40, `morning.txt` 15→23,
  `afternoon.txt` 15→22, `evening.txt` 15→23). Added [TIP_SOURCES.md](TIP_SOURCES.md), which
  cites a primary source (PubMed/PNAS/Annals of Internal Medicine/etc.) for every
  non-obvious claim across all four pools, organized by theme. New/edited tip lengths were
  kept within the existing catalog's envelope (~50-115 chars) since the widget caps tip text
  at `maxLines = 5` in a bold, centered 16sp font (`TipWidget.kt`) that also has to fit the
  smallest home-screen widget size.
- **`feat:` settings screen redesign + widget styles + wider anti-repeat window
  (2026-07-24, later same day)** — by request: "more visual, less text, more freedom, more
  settings" for the settings screen, plus the same tip shouldn't repeat within a 30-tip span
  instead of just not back-to-back. Concretely:
  - Every settings section now has a leading icon (`material-icons-extended`, justified in
    a build.gradle.kts comment since it's beyond the originally-fixed stack) and shorter
    body text; the app's own launcher mark now sits next to the screen title.
  - New **Widget** section: a live preview (rendered from the actual widget background
    drawable, not a hand-picked brush) plus four selectable background styles
    (Forest/Ocean/Sunset/Midnight — `WidgetStyle` enum in `:core`, actual drawables/colors
    in `:app`), wired into `AppSettings`/`SettingsRepository` alongside the existing
    fields. Changing the style persists it and calls `WidgetScheduler.refreshNow()`
    (runs the update inside a WorkManager job so it isn't tied to this screen's
    lifecycle). The refresh cadence is a fixed 2h default in `WidgetScheduler` now —
    the user-facing "Refresh every" control was removed (2026-07-24, later still) along
    with `WidgetRefreshInterval` and its DataStore key.
  - Settings sections are now grouped into `Card`s (`SectionCard`) instead of flat
    `HorizontalDivider`-separated blocks, and the widget style swatches/preview render the
    real layered gradient drawables via `AndroidView`/`ImageView` interop (Compose's
    `painterResource` doesn't support `<layer-list>`/`<shape>` resources).
  - Nudge frequency ceiling raised from 3/day to 6/day (`AppSettings.MAX_NOTIFICATION_FREQUENCY`),
    with new default nudge times for levels 4-6 in `NudgeScheduler.NUDGE_TIMES_BY_FREQUENCY`,
    and the frequency label switched from three hardcoded strings to a proper `<plurals>`
    resource so it scales to any count.
  - Anti-repeat generalized from "exclude the single last tip" to "exclude the last
    `TipHistoryRepository.MAX_RECENT_TIPS` (30) tips": the repository interface changed from
    `lastTip: Flow<String?>` / `setLastTip()` to `recentTips: Flow<List<String>>` /
    `recordTip()`, `TipEngine.messageFor` now takes `recentTips: List<String>` instead of a
    single nullable, and `DataStoreTipHistoryRepository` stores the list as a
    newline-joined string (tips are always single-line, so no JSON dependency needed) and
    trims to the 30 most recent on every write.
- **`feat:` tip source citations + settings screen polish (2026-07-24, later still)** —
  every tip now carries a citation, and a real bug in the widget-style picker got fixed:
  - New `Tip` data class (`text`, `sourceLabel`, `sourceUrl`) replaces the raw `String` that
    `TipCatalog`/`TipEngine`/`AdvanceTipUseCase` used to pass around. Each pool `.txt` file
    now has a companion `<name>_sources.txt` (`Label<TAB>URL` per line, same order),
    zipped in by `TipCatalog.loadDefault()`; `TipCatalogTest`'s "every tip has a real
    citation" test fails the build if a label or URL is blank. `TipEngine.findByText`
    resolves the persisted (plain-text) history back to a full `Tip` for display.
  - Settings screen gained a "Why this tip?" card (`TipSourceSection`) showing the current
    tip's citation with a button that opens `sourceUrl` in the system browser
    (`Intent.ACTION_VIEW`) — no `INTERNET` permission needed for that, since the browser
    process makes the request, not this app.
  - **Real bug found and fixed, in two attempts**: changing the widget style worked the
    *first* time, then silently stopped working until the app was force-stopped and
    relaunched. Root cause: the style-change handler called `TipWidget().updateAll(context)`
    directly inside the settings screen's own `rememberCoroutineScope()`-backed coroutine,
    which is cancelled if the user navigates away before the Glance composition finishes —
    apparently leaving that widget ID's Glance session stuck until the process died.
    - First attempt: routed the call through `WidgetScheduler.refreshNow()` (a one-time
      `WorkManager` job), mirroring how `WidgetRefreshWorker` drives the periodic refresh.
      This fixed the "stuck after one change" symptom (confirmed on-device), but introduced
      a new problem — the user reported a multi-second **lag** before the background
      actually changed, and sometimes it didn't visibly change at all within the time they
      were watching. `WorkManager`'s scheduler doesn't guarantee near-instant execution for
      a plain (non-expedited) one-time request, which is fine for a background periodic
      refresh but not for something tapped expecting to see change immediately.
    - Second attempt (current): call `TipWidget().updateAll()` directly again, but inside
      `HealthWidgetApp.applicationScope` (a process-lifetime `CoroutineScope`, now
      `internal` instead of `private`) rather than the screen's own scope. This keeps the
      original problem fixed (the scope outlives the screen) while being as fast as the
      original direct call (no `WorkManager` queueing). `setExpedited()` on the
      `WorkManager` request was considered and rejected: it requires overriding
      `getForegroundInfo()` (i.e. showing a system notification) on API < 31 or the app
      crashes there, which is a non-starter for a purely cosmetic background refresh on an
      app whose whole philosophy is minimal, unobtrusive notifications.
    - Not yet re-confirmed on-device after this second fix — reinstalled, awaiting the
      user's repeat of the "tap through several styles in a row" test.
  - The user-configurable widget refresh interval (1h/2h/4h, `WidgetRefreshInterval`) added
    earlier the same day was removed again: FR1 only requires "at least every 2 hours," and
    the setting wasn't earning its keep. `WidgetScheduler` is back to a hardcoded 2h.
  - Settings sections are now grouped into `Card`s (`SectionCard`) instead of flat
    `HorizontalDivider`-separated blocks, and the widget preview/style swatches render the
    *actual* layered gradient drawables (radial glow + decorative sparkle/star/ember accents
    added to each `widget_background_*.xml`, not flat two-stop gradients) via
    `AndroidView`/`ImageView` interop, since Compose's `painterResource` doesn't support
    `<layer-list>`/`<shape>` resources — `WidgetStyle.backgroundDrawableRes()` (in
    `TipWidget.kt`, now `internal`) is shared between the real widget and this preview so
    they can't drift apart.
- **`feat:` notifications master switch, manual tip refresh, settings restructure
  (2026-07-24, later still)** — by request:
  - New `AppSettings.notificationsEnabled` (default `true`) is a true kill-switch, not just
    "frequency = 0": `NudgeScheduler.apply()` and `SleepAlertWorker` both gate on it ahead of
    the existing frequency/sleep-alert checks, so turning it back on restores whatever
    cadence was already dialed in rather than losing it. New DataStore key
    (`notifications_enabled`) + repository method + test.
  - Frequency/Sleep alert/Quiet hours collapsed from three separate `SectionCard`s into one
    **Notifications** card (`NotificationsSection`) with the master switch in its header row;
    the sub-controls drop to a plain `SubsectionTitle` (no icon) instead of a full
    `SectionTitle` each, and the whole card reduces to a one-line "off" hint when the switch
    is off. Sections reordered: Notifications → Widget → current-tip card → About (functional
    controls first, boilerplate last).
  - "Why this tip?" gained a refresh `IconButton` (`Icons.Filled.Refresh`) that calls the
    same `AdvanceTipUseCase` the periodic worker uses, then `TipWidget().updateAll()` via
    `HealthWidgetApp.applicationScope` — picks a new tip out of turn and pushes it to the
    home-screen widget immediately, same instant-update reasoning as the widget-style fix.
  - About is now collapsed by default (tap the row to expand/collapse; a plain `if`, no
    `AnimatedVisibility`, to avoid pulling in `androidx.compose.animation` on a fixed stack),
    and its copy was rewritten from a "no X, no Y, no Z, not even W" listy paragraph to two
    short clauses ("Everything stays on your phone — tips, settings, history. No account, no
    tracking, no ads.").
  - The four widget background drawables had drifted toward looking like the same template
    recolored: Forest's end color was blue-gray (`#3A5F72`), landing in the same cool-blue
    family as Ocean and Midnight, and all four used an identical 135° base-gradient angle.
    Forest's palette is now genuinely green (`widget_gradient_start/end` renamed to
    `widget_forest_start/end`, end color to a near-black forest green `#0B2E1F`, its "light"
    accent from icy mint to warm dappled gold), and each theme got a distinct base angle
    (Forest 135°, Ocean 90°, Sunset 225°, Midnight 315°) so they read as different scenes,
    not just different color stops on the same template.
- **`feat:` DST/time-zone-safe scheduling, concurrency-safe tip advancement, backup-rules
  fix, branding centralization, API 36 (2026-07-24, later still)** — a release-readiness
  pass covering six things:
  - **Scheduling is now DST/time-zone/clock-change safe.** `durationUntilNext` (`:core`)
    moved from `LocalTime` + a naive 24h rollover to `ZonedDateTime` + an injectable `Clock`,
    so a pending nudge or sleep alert lands on the intended wall-clock time across a DST
    transition or a manual clock/zone change instead of literally "24 hours from now." New
    `ClockChangeReceiver` listens for `ACTION_TIMEZONE_CHANGED`/`ACTION_TIME_CHANGED` and
    fully recomputes and replaces scheduled work (not just tops it up); `BootReceiver` was
    changed to do the same full recompute on boot, since the clock/zone can plausibly have
    changed during downtime. See README "Notable design decisions" for the DST-gap/overlap
    behavior and `NextOccurrenceTest` for the covering unit tests (normal/rolled-over/
    already-passed occurrences, spring gap, autumn overlap, and a same-instant/different-zone
    case — all pure JVM, no Android needed).
  - **Tip advancement is now concurrency-safe.** `AdvanceTipUseCase` wraps its
    read-history/select/persist sequence in a `Mutex`, closing a real read-modify-write race
    between e.g. a widget refresh and a notification firing at the same moment. Verified with
    a test that deliberately removed the mutex first to confirm it actually fails without it
    (it does), then restored it.
  - **Found and fixed a real bug while reasoning through the above**: `NudgeWorker` never
    checked `AppSettings.notificationsEnabled` (only quiet hours) before notifying and
    rescheduling itself, unlike `SleepAlertWorker`, which already gated on it. Fixed to match.
  - **Backup rules were technically broken and have been fixed.** Both `backup_rules.xml` and
    `data_extraction_rules.xml` targeted `domain="sharedpref"`, which matches nothing — the
    app has no `SharedPreferences`, only Preferences DataStore under `files/datastore/`. Both
    now use `domain="file" path="datastore/"`. Policy kept as "back up via Android's system
    backup" (consistent with the project's existing framing), but `PRIVACY.md` was rewritten
    to stop implying nothing ever leaves the device — system backup is the one real exception,
    and it's now stated plainly rather than glossed over.
  - **Branding centralized.** `settings_title` now references `@string/app_name` instead of
    duplicating the literal "HealthWidget," and the widget's small brand label (previously a
    hardcoded `"HEALTHWIDGET"` string in `TipWidget.kt`) is now `app_name` uppercased at
    render time. A pre-release checklist warning about `applicationId` being permanent once
    published was added to "Release readiness" below. `applicationId` itself was
    deliberately left unchanged (`com.healthwidget.app`) — no final name has been chosen yet.
  - **API 36.** AGP 8.7.3 to 8.10.1 (the minimum AGP version that supports `compileSdk 36`),
    Gradle wrapper 8.9 to 8.11.1 (AGP 8.10.1's minimum required version), `compileSdk`/
    `targetSdk` 35 to 36, `minSdk` unchanged at 26. Android SDK Platform 36 and Build-Tools 35
    were auto-downloaded by AGP into the scratch SDK directory on first build. No other
    dependency versions were bumped (lint flags several as outdated — `androidx.core`,
    `lifecycle`, `activity-compose`, the Compose BOM, WorkManager, DataStore — all left as-is
    since none were required for API 36 compatibility, matching the "don't blindly upgrade
    everything" constraint). No manifest, notification, widget, or edge-to-edge lint issues
    resulted from the bump; the merged manifest was inspected directly and still declares no
    `INTERNET` permission.
  - Android-side lifecycle/worker test coverage (Robolectric, WorkManager testing APIs) was
    written and passing — `NudgeScheduler` no-duplicate-work, `NudgeWorker`/`SleepAlertWorker`
    quiet-hours/permission-denial/notifications-disabled behavior, `BootReceiver`/
    `ClockChangeReceiver` rescheduling — but was then **deliberately dropped by request**
    before landing, along with the handful of test-only production changes it needed
    (`HealthWidgetApp` made `open` for a test subclass, `WidgetScheduler`'s work-name
    constants made `internal`, `BootReceiver`/`ClockChangeReceiver` given an injectable
    `CoroutineScope`, a `junit-vintage-engine` dependency to run JUnit4-style Robolectric
    tests on the project's JUnit5 platform setup). All of that was reverted; only the `:core`
    tests above and the `NudgeWorker` bug fix remain. If Android-side test coverage is wanted
    later, the approach above (documented here in case it's picked back up) worked and all
    tests passed before being removed.
- **`fix:` tap-to-refresh sleep-hours bug, widget background now follows the tip, background
  picker removed from Settings, bigger gear icon (2026-07-24, later still)** — by request:
  - **Found and fixed the real cause of "tapping the widget doesn't change the quote."**
    `TipEngine.messageFor` returns a single fixed message for the two sleep day parts
    (`DayPart.SLEEP_LATE` 23:00, `DayPart.SLEEP_EARLY_HOURS` 00:00-05:59) with no
    randomization at all — by design, for the *passive* scheduled rotation (there's only one
    possible message for each, so anti-repeat doesn't apply). But that exemption also applied
    to an explicit tap, so tapping the widget (or the Settings refresh button) during that
    ~7-hour window was a silent no-op: the same fixed text came back every time. Fixed by
    adding a `manual: Boolean = false` parameter to `TipEngine.messageFor` and
    `AdvanceTipUseCase.invoke`: when `true` (now passed by `RefreshTipAction` and the
    Settings screen's refresh button), the sleep day parts draw from the general pool instead
    of the fixed message. The passive worker (`WidgetRefreshWorker`) and the widget's
    first-render fallback keep the default `false`, so the wind-down message still shows
    normally when nothing was explicitly requested. Covered by new tests in
    `TipEngineTest`/`AdvanceTipUseCaseTest`.
  - **The widget's background is now tied to the current tip, not a stored preference** — by
    request ("there is a bug that changes the background when a different tip appears - keep
    it and make it a feature"). New `WidgetStyle.forTip(tipText: String)` (`:core`,
    `Math.floorMod(tipText.hashCode(), 4)`) deterministically maps a tip's text to one of the
    four existing styles: the same tip always renders the same background, and a different
    tip (almost always) means a different one. `TipWidget.provideGlance` now calls
    `WidgetStyle.forTip(tip)` instead of reading a persisted setting.
  - **Background choice removed from Settings** — with nothing left to persist, the entire
    settings-persistence layer was deleted: `AppSettings`, `SettingsRepository`,
    `DataStoreSettingsRepository` (+ its test and DataStore key), and `WidgetSection`/
    `WidgetPreview`/`StyleSwatch`/`WidgetBackgroundImage` from `SettingsScreen.kt`.
    `AppContainer` no longer exposes a `settingsRepository`. `SettingsScreen` now takes just
    `tipHistoryRepository`/`tipEngine`. README/PRIVACY.md updated to match (mermaid diagram,
    module descriptions, stored-data list).
  - **Widget's settings gear icon made slightly bigger** — `20.dp` to `24.dp` in
    `TipWidget.kt` (padding unchanged at `6.dp`).
  - Verified via `:core` test (41 tests, up from 37 — new `WidgetStyleTest` plus the new
    manual-advance cases), `:app` test (5 tests per variant, unchanged), `ktlintCheck`,
    `lint` (0 errors, 17 pre-existing warnings, same as before), and a full `build` including
    `assembleRelease` with R8 minification — all green, against the same JDK 17 + Android SDK
    scratch setup as prior passes. **Not yet verified on a physical device** — this session
    did not have device/adb access either; the sleep-hours tap fix in particular needs a
    real on-device tap during 23:00-05:59 (or a temporarily-adjusted system clock) to confirm,
    and the tip-follows-background behavior needs eyes-on confirmation that it reads as a
    deliberate visual refresh rather than a jarring flicker.
- **`feat:` notifications removed entirely, tip advancement retimed to ~90 minutes of
  confirmed screen-on time, and the widget-refresh race fixed for real (2026-07-24, later
  still)** — by request ("I don't want the app to create distractions"):
  - **Every notification code path is gone.** Deleted: `NudgeScheduler`, `NudgeWorker`,
    `SleepAlertWorker`, `NotificationHelper`, `ClockChangeReceiver`, `QuietHours`,
    `durationUntilNext`/`NextOccurrence` (dead once nothing computes a daily reschedule delay
    any more), and their tests. `AppSettings` is down to just `widgetStyle`;
    `SettingsRepository`/`DataStoreSettingsRepository` lost every notification-related
    method and DataStore key. Removed from the manifest: `POST_NOTIFICATIONS`, the
    `ClockChangeReceiver` receiver declaration. `HealthWidgetApp.onCreate` and `BootReceiver`
    no longer touch notification channels or scheduling — `BootReceiver` doesn't even need
    `AppSettings` any more, since `WidgetScheduler` takes none. The Settings screen lost its
    entire Notifications card (master switch, frequency slider, sleep alert toggle, quiet
    hours time pickers, the permission-rationale card) along with every string and icon that
    only that card used. `sleepLate`/`sleepEarlyHours` tip pools are untouched — they're a
    normal part of `TipEngine.dayPartFor`'s time-of-day rotation (23:00-05:59), not
    notification-specific, so the widget still shows a wind-down tip late at night, just via
    the ordinary refresh path instead of a dedicated alert.
  - **Tip advancement is retimed from a fixed periodic clock to "~90 minutes of confirmed
    screen-on time since it was last seen."** The stated goal was specifically not wanting a
    tip to change without the user having actually seen the previous one — a pure wall-clock
    timer could rotate one nobody ever looked at (screen off overnight, phone face-down all
    afternoon). There's no "notify me when the screen turns on" WorkManager primitive, and a
    real screen-on/off `BroadcastReceiver` can't survive process death without a foreground
    service (rejected as overkill for a passive widget), so `WidgetRefreshWorker` instead
    ticks every 15 minutes (WorkManager's own periodic-interval floor,
    `TICK_INTERVAL_MINUTES` in new `core/scheduling/TipRefreshSchedule.kt`) and only counts a
    tick if `PowerManager.isInteractive` is true at that instant; `shouldAdvanceTip` advances
    the tip once 6 such ticks (`TICKS_UNTIL_ADVANCE`) have accumulated since it was last
    shown — a sampling approximation (a tick reflects only the instant it fired, not the
    whole interval before it), not a precise stopwatch, but it needs no extra permissions, no
    live receiver, and the count persists (new `WidgetRefreshRepository`/
    `DataStoreWidgetRefreshRepository`, mirroring the existing repository-interface-in-`:core`
    pattern) rather than living in memory, so it survives the process dying between ticks.
    `WidgetScheduler`'s periodic job dropped from a 2-hour interval to the new 15-minute tick;
    `WidgetRefreshWorker` gained a `KEY_FORCE` input flag so `refreshNow()` (used right after
    boot) can still force an immediate *re-render* of the widget's current state without that
    counting as (or requiring) a tick. Covered by new `TipRefreshScheduleTest` (`:core`, pure
    JVM).
  - **Found and fixed the real cause of the "widget background only sometimes updates"
    bug**, which the user confirmed was still happening intermittently. The previous fix
    (see the two-attempts writeup earlier in this file) made a single style-change call safe
    from the screen's own coroutine scope, but never addressed that `TipWidget().updateAll()`
    was being called from **four independent, uncoordinated places** once the widget's own
    tap-to-refresh action shipped: the periodic worker, a Settings style change, the manual
    "get a different tip" button, and now tapping the widget itself. With no ordering
    guarantee between them, two overlapping calls could finish out of order and leave a
    stale render on screen — worse now than before, since the widget's own refresh action
    made overlaps far more likely (e.g. tapping the widget right as the periodic tick fires).
    Fixed by adding `AppContainer.refreshWidget()`, a `Mutex`-guarded single entry point that
    all four call sites now go through instead of calling `TipWidget().updateAll()` directly.
    Each call still re-reads current settings/tip from DataStore at execution time rather
    than a captured snapshot, so serializing the push alone is sufficient: whichever call
    runs last always renders whatever is actually persisted, regardless of which trigger
    queued first — no change needed to `AdvanceTipUseCase`'s own separate mutex.
  - README, PRIVACY.md, and CONTRIBUTING.md updated to match: the architecture diagram and
    "Notable design decisions" no longer mention any removed notification component, the v1
    scope list and privacy policy no longer describe notification permissions or settings
    that don't exist any more, and the tip-length guidance in CONTRIBUTING.md dropped its
    "read at a glance in a notification" framing.
- **`feat:` multi-source citations, near-duplicate tip rewrite, source credibility audit
  (2026-07-26)** — by request ("a lot of tips say the same thing"; "only one source per tip is
  not enough proof"; "check whether the sources are credible"), plus the tip-related roadmap
  items:
  - **`Tip` now carries a `List<TipSource>` instead of one `sourceLabel`/`sourceUrl` pair.**
    New `TipSource(label, url)`; `Tip.MIN_SOURCES` (2) is the floor. A `*_sources.txt` line is
    now one or more `Label<TAB>URL` pairs tab-separated end to end (even field count), which
    keeps the existing line-N-of-tips ↔ line-N-of-sources invariant intact rather than needing
    a new file format. `TipCatalogTest` enforces the floor, HTTPS URLs, and that a tip's own
    sources are distinct from each other (citing one URL twice would game the count).
    Settings' "Why this tip?" card lists every source as its own tappable row under a
    "Backed by N sources" `<plurals>` header, instead of only the first.
  - **Near-duplicate audit done** (roadmap item). Roughly a third of the catalog rewritten.
    Verbatim-ish pairs deleted outright (two "outdoor light is brighter than indoors" morning
    tips; two "a short chat lifts the afternoon lull" tips). Remaining clusters differentiated
    by *register* instead of merged, so an action tip and the evidence tip behind it now say
    different things. Pool sizes stayed roughly flat (41/23/22/23) because duplicates were
    replaced with new distinct content rather than just removed.
  - **Em dashes removed from all bundled tip content** (roadmap item), with a
    `TipCatalogTest` case so it can't regress. `strings.xml` already had none. Docs keep them
    deliberately: maintainer prose, not app-facing text.
  - **The credibility audit found four real content bugs, not just weak links.** The worst:
    the snooze tip ("repeatedly hitting snooze can leave you groggier") was *contradicted by
    its own cited source* — Sundelin et al. 2024 found 30 min of snoozing improved or didn't
    affect cognition on rising and cost ~6 min of sleep. Also: the afternoon carb-crash tip
    was cited to **Wikipedia** and overstated evidence that Orr et al. 1997 actually found
    null; the Monk citation was labelled *Chronobiology International* while linking the 2005
    *Clinics in Sports Medicine* paper; and TIP_SOURCES.md claimed walking "roughly doubled"
    creative output when Oppezzo & Schwartz report ~60%. All four fixed.
  - Sources dropped for quality and replaced: Wikipedia, ResearchGate, EngineeringToolBox,
    ScienceDirect Topics (auto-generated aggregation), IFIC (food-industry-funded), a possibly
    dead Ovid deep link, and the AOA's 5MB infographic PDF. Press-release links (ScienceDaily,
    Stanford News, Penn State, Baylor, UT Austin) are kept only *alongside* the primary study,
    never instead of it. Every URL in the catalog was checked this pass.
  - TIP_SOURCES.md was restructured around this: a "How citation works here" section stating
    the source-quality rules, a "Corrections made in this pass" section recording the four
    fixes above, then the per-theme writeups.

- **`feat:` motivation/philosophy/wellbeing tone pools + `TipKind` (2026-07-26, later same
  day)** — by request. The three tone pools the roadmap had been holding open finally have
  content, and the citation model was split per-kind so they could exist honestly:
  - **`TipKind` (`PRACTICAL`/`MOTIVATION`/`PHILOSOPHY`/`WELLBEING`) is the roadmap's long-
    deferred item, done as planned alongside the content rather than before it.** The blanket
    "every tip needs 2+ research citations" rule is right for "mild dehydration dents focus" and
    nonsense for "you're allowed to begin again"; forcing the latter through it would have meant
    inventing or stretching a citation, which is the exact dishonesty the rule exists to
    prevent. `TipKind.requiresCitation` is true only for `PRACTICAL`, and `TipCatalogTest`
    checks each rule separately (practical tips have 2+ sources; motivation/wellbeing tips have
    *zero*, so none is implied; philosophy has both quoted and unquoted entries; no tip text
    appears in two pools).
  - **Three pools, 54 tips**: `motivation.txt` (18), `philosophy.txt` (18), `wellbeing.txt`
    (18). `TipCatalog.motivation`/`philosophy`/`wellbeing` replace the never-populated
    `philosophical`/`lighthearted` fields; `TipCatalog.tonePools` concatenates them.
  - **Quotations only where the attribution is checkable.** Five of the philosophy tips are
    public-domain quotations, each cited to a specific chapter or letter (Epictetus *Enchiridion*
    I and V, Seneca *Letter 13*, Marcus Aurelius *Meditations* VII, *Tao Teh King* 64) via
    Gutenberg/Wikisource/Perseus, all verified this session. Popular philosophy quotes are
    misattributed constantly — the thousand-miles line gets pinned on Confucius, several famous
    Marcus Aurelius one-liners are modern paraphrases with no matching passage — so anything
    that couldn't be pinned to a passage was written as an original reflection instead.
  - **Tone pools use an inline attribution format** (`Text<TAB>Label<TAB>URL` on the tip's own
    line) rather than the practical pools' companion `_sources.txt`. Deliberate asymmetry: most
    tone lines have no source, so a parallel file would be almost entirely blank, and blank
    lines are exactly what the loader strips — the two files would silently stop lining up.
  - Settings' "Why this tip?" card is now kind-aware: "Backed by N sources" for practical,
    "Quoted from" for an attributed quotation, no citation block at all for an original line,
    and the tip's group shown next to the day part ("EVENING · PHILOSOPHY") for tone tips only.
  - **Behaviour change worth flagging**: the default `VarietyLevel` is `PRACTICAL` = 20% tone
    chance, and the tone pools were previously *empty*, so `selectGroup` always short-circuited
    to the practical pool. Now ~20% of tips shown are tone tips by default, where it was 0%
    before. Whether 20% is right at the `PRACTICAL` level is an open question.
  - **The selection algorithm was deliberately not touched**, by request. All three tone pools
    are still weighed as one undifferentiated blob with no time-of-day awareness, so a `PLAYFUL`
    draw is equally likely to be a Stoic quotation, a motivational push, or a self-compassion
    check-in. A self-contained brief for that work is in
    [docs/TONE_ALGORITHM_PROMPT.md](docs/TONE_ALGORITHM_PROMPT.md), covering the current model,
    the load-bearing properties not to break, the open questions, and the definition of done.
  - **Revised the same day after review.** The first draft of all three pools was wrong in
    ways worth recording, and each pool now carries its writing rule as a header comment:
    - **`wellbeing.txt` was rewritten completely.** Nearly every line presupposed the reader
      was struggling ("it's fine if today was mostly getting through it", "if everything feels
      heavy...") and several were quietly corrective ("nobody is watching you as closely as you
      think" = you are wrong about something). On a widget that appears at random, that reads
      as an unasked-for diagnosis. Replaced with small concrete invitations that work just as
      well on a good day: hands around something warm, ten seconds out of a window, the song
      you always skip back to. Also much less abstract than the first draft.
    - **`motivation.txt` was rewritten to actually push.** Half the first draft was
      permission-giving ("rest is part of the work", "not every day has to be productive"),
      which is warm but is wellbeing's job, not motivation's. Now short sentences, verbs up
      front, real imperatives.
    - **`philosophy.txt` went from 5 quotations to 14**, with originals down to 4. New
      `TipCatalogTest` case keeps the pool majority-quotation so it can't drift back.
      All new quotes verified to a specific chapter/letter in a public-domain edition
      (Epictetus I/V/VIII, Meditations IV.3 ×2, VII, VIII.47, Seneca 1 and 13 ×2, Tao Teh King
      33 and 64, Plato's *Apology*, Nietzsche). **Verification caught a real misattribution
      mid-draft**: the famous "he who has a why to live can bear almost any how" is Viktor
      Frankl's paraphrase, not Nietzsche's sentence — *Twilight of the Idols* actually reads
      "If we have our own why in life, we shall get along with almost any how," which is what
      shipped.
    - A wording pass over the four practical pools too (16 lines), trading vague phrasing for
      concrete instruction: "gently stretch your neck from side to side" became "tip one ear
      toward your shoulder and hold ten seconds", "check in on your water intake" became "when
      did you last actually drink something? Coffee only half counts."
  - Verified via `:core` tests (54 passing, including 6 new `TipCatalogTest` cases),
    `ktlintCheck`, `lint` (0 errors, 17 warnings, unchanged), and a full `build` including
    `assembleRelease` with R8. **Not verified on a physical device**: nothing here has been seen
    rendered — in particular the new kind label in the card header, the "Quoted from" variant,
    and how a tone tip actually reads on the widget at the moment it appears.
- **`docs:` tone pools roughly doubled and de-cliché'd (2026-07-27)** — by request ("make them
  better, more varied and interesting"). Content only; no Kotlin changed, no test weakened.
  - **54 tone tips to 111**: `motivation` 18 → 38, `philosophy` 18 → 36, `wellbeing` 18 → 37.
  - **The real problem in each pool was sameness, not size.** Each had filled up with whichever
    line is easiest to write in that voice, so a big pool read like a small one:
    - **`motivation.txt` was ten-of-eighteen "just start" lines.** Starting is now capped at
      seven, and the additions cover the angles it was missing entirely: the boring middle,
      coming back after a lapse, finishing, reps over talent, comparison, saying the awkward
      thing out loud, and keeping your own word. Two of the weakest starting lines were cut as
      restatements ("Pick the smallest possible version of it" duplicated "Make the first step
      so small it's embarrassing"; "The hard part ends the second you begin" duplicated "The
      first five minutes are the whole fight").
    - **`wellbeing.txt` was mostly quiet indoor noticing prompts.** All 18 kept; 19 added to
      spread it across the body, outside, play, other people, and small acts of making things
      nicer. New body lines are phrased as invitations ("Let your shoulders drop about a
      centimetre") rather than corrections ("your shoulders are higher than you think"), since
      the file's own rule forbids telling the reader they were wrong about something.
    - **`philosophy.txt` was 100% Stoic-plus-a-tail**, which is a school, not a subject: four
      Marcus, three Epictetus, three Seneca, two Lao Tzu, one Socrates, one Nietzsche. Now 28
      quotations across Stoic, Taoist, Confucian, Buddhist, Greek, essayist/transcendentalist
      and German, plus 8 originals (up from 4).
  - **Every new quotation was verified against the linked edition's own text, not a quotations
    site.** Method: download the public-domain text (Gutenberg plain text, or Wikisource via
    `action=parse` for proofread pages that transclude from `Page:` namespace) and `grep` for
    the exact sentence. This rejected two candidates that a quotations site would have waved
    through — Boethius' "nothing is miserable unless you think it so" is absent from the Cooper
    translation in that wording, and Emerson's "envy is ignorance; imitation is suicide" is
    mid-sentence in *Self-Reliance* ("that envy is ignorance; that imitation is suicide"), so
    three cleanly-standing Emerson lines were used instead. New sources: Confucius' *Analects*
    (Legge, Gutenberg 3330), the *Dhammapada* (Müller, Gutenberg 2017), Emerson's
    *Self-Reliance*, Thoreau's *Walden* (two chapters), Mill's *Autobiography* ch. V, Helen
    Keller's *Optimism*, and Montaigne's *Essays* I.38 (Cotton) — all Wikisource except the two
    Gutenberg texts, and all 8 new URLs checked for HTTP 200.
  - **Each tone file gained a second header rule, about variety**, since nothing automated
    catches a pool drifting towards one angle and all three drifted the same way. Tips are now
    grouped under `#` headings by angle/tradition so a thin group is visible at a glance;
    those headings are stripped at load time like any other comment and don't affect selection.
    CONTRIBUTING.md documents all three rules.
  - Verified via `./gradlew ktlintCheck test` (green; `TipCatalogTest` 13/13, which covers the
    citation rules, cross-pool duplicate text, em dashes, and philosophy staying
    majority-quotation). **Not verified on a physical device**: no new line has been seen
    rendered on the widget, and the longest new tip (Thoreau's "I went to the woods...", 100
    chars) is close to the longest tip already shipping (111), so it should fit the 6-line
    limit, but that is inference from length, not something anyone has looked at.
- **`docs:` practical pools grown, tone pools given a sense of humour (2026-07-27, later same
  day)** — the two remaining *content* roadmap items for the tip pools, done together. Content
  only; no Kotlin changed, no test weakened. The other two tip-pool roadmap items were
  deliberately **not** touched: turning the sleep-hours messages into pools, which README says
  to fold into the tone-algorithm work rather than do alone, and the selection rework itself,
  whose brief asks for a discussion of the model before any code.
  - **Practical pools 109 tips to 119**: `general` 41 → 45, `morning` 23 → 24, `afternoon`
    22 → 24, `evening` 23 → 26. Chosen to fill *topic* gaps rather than restate covered
    ground, since the previous pass established the pools were dense with near-duplicates:
    time outdoors, social connection, acts of kindness, dietary fibre, muscle-strengthening,
    short vigorous bursts, music for stress, evening exercise timing, night mode vs screen
    brightness, and short sleep vs colds.
  - **Every new tip carries two independent citations, verified against the primary
    literature** rather than a summary. Method as before: search, then read the abstract or
    full text, then confirm the identifier resolves to the paper being cited (all seven new
    PubMed IDs were fetched and title-checked). Three things this caught:
    - **Two of the best sources disagree**, and the tip is worded to survive both. Stutz et al.
      (2019) found evening exercise does not harm sleep, caveat vigorous exercise ending under
      *an hour* before bed; Frimpong et al. (2021) drew the line at *two hours* for
      high-intensity work. The tip says "an hour or two" rather than picking a side.
    - **The night-mode tip needed to not contradict an existing one.** `evening.txt` already
      says blue-enriched light suppresses melatonin more than warm light *of the same
      brightness*. Nagare et al.'s finding is that a filter alone is insufficient *without
      dimming*. Both are true and they sit alongside each other; TIP_SOURCES.md now says so
      explicitly so a future pass doesn't "fix" one of them.
    - **The fibre tip's two halves come from different source types on purpose.** The 25-30g
      figure is Reynolds et al.'s Lancet meta-analysis; "most people fall short" is NHS
      population-intake guidance, used as the guidance source alongside a primary study exactly
      as the source-quality rules in TIP_SOURCES.md allow.
  - **Lightness, done as pure content** exactly as the roadmap predicted (no new `TipKind`, no
    new pool, no structural work): `wellbeing.txt` gained a **Lighter** group (8 lines) and
    `motivation.txt` a **Wry** group (5). Both files gained a third header rule, because the
    roadmap's "hard to do without being grating on a bad day" worry is real and each pool's
    existing rules already imply the answer: humour that needs the reader to be having a bad
    day to land breaks wellbeing's no-presupposing rule, humour at the reader's expense breaks
    its no-correcting rule, and a wry line that gets a laugh without pushing belongs in
    wellbeing rather than motivation.
  - Tone pools are now `motivation` 43, `philosophy` 36, `wellbeing` 45 (124 total).
  - **Verified via `./gradlew test`: BUILD SUCCESSFUL, `TipCatalogTest` 13/13.** A custom
    validation pass also re-checked every pool and `_sources.txt` file for line-for-line
    alignment, even field counts, 2+ sources per practical tip, HTTPS URLs, no URL repeated
    within one tip, no em dashes, and no duplicate text across pools.
  - **`ktlintCheck` currently fails, and not because of this work.** Every one of the 28
    violations is in `TempSimulationTest.kt` (untracked scratch file, header says "TEMPORARY,
    deleted before commit") and `TipEngineTest.kt` — both files belonging to the in-flight
    tone-algorithm rework happening in this same working tree (`ToneProfile.kt`,
    `ToneProfileTest.kt`, and a modified `TipHistoryRepository.kt` also appeared mid-session).
    This pass touched no Kotlin at all. Left alone rather than reformatted, since editing
    someone else's in-progress files would conflict with that work.
  - **Not verified on a physical device**: no new tip has been seen rendered. The longest new
    line is the sleep-and-colds tip at 104 characters, within the ~115 guidance in
    CONTRIBUTING.md but the longest in the catalog, so it is the one to look at first if
    anything clips at 6 lines.

- **`feat:` tip selection reworked around time of day and recency (2026-07-27)** — the
  roadmap's long-deferred selection pass, from the brief in
  [docs/TONE_ALGORITHM_PROMPT.md](docs/TONE_ALGORITHM_PROMPT.md). `TipEngine` went from a
  two-way weighted coin flip over concatenated pools to three narrowing weighted choices:
  - **Anti-repeat is now structurally applied before every weighted choice**, not per-group by
    hand. `availableTiers` filters each group against the window, drops anything with nothing
    fresh, and redistributes its share among the survivors; only when *nothing* anywhere is
    unseen does it fall back to the unfiltered pools. The old code special-cased the two groups
    that existed with an explicit `when`; going to five groups is exactly how that bug would
    have come back, so the fix was generalized rather than extended. The original regression
    test still passes untouched, plus a new group-level counterpart.
  - **The three tone pools are split by day part** (`ToneProfile.forDayPart`, new file in
    `:core`) instead of being concatenated into one undifferentiated blob: motivation 5/3/2
    against philosophy/wellbeing in the morning, 4/2/4 in the afternoon, 1/4/5 in the evening,
    and **0**/4/6 at night. That zero is the only place the table filters rather than leans, and
    is argued in the file and pinned by `ToneProfileTest` — a "Two minutes. Set a timer. Go." at
    3am isn't a weaker version of good advice, it's the opposite of what the hour calls for. It
    doesn't contradict "variety is a lean, not a filter," which is a promise about the *user's
    setting*; no `VarietyLevel` silences anything.
  - **Night stopped being one fixed message.** 23:00-05:59 now runs through the same machinery
    as every other day part, with the wind-down message as a one-tip practical group (still
    anti-repeat exempt) weighed against philosophy and wellbeing at a higher tone share than
    daytime (50/70/85 vs 20/50/80), since its practical side isn't a pool and the daytime split
    would mean the identical sentence four nights in five. Measured at the default level: 50%
    wind-down, 28% wellbeing, 20% philosophy, 0% motivation.
  - **`general` and the day-part pool now split the practical share evenly.** They used to be
    concatenated and drawn from uniformly, so file sizes set the ratio — `general` (45) against
    a day-part pool (~24) meant ~65% of practical draws came from the most time-neutral content
    in the catalog. This is the change most likely to be felt, and it wasn't in the brief.
  - **Uniform random within a pool became recency weighting**, which needed
    `MAX_RECENT_TIPS` 30 → 90 with a new separate `ANTI_REPEAT_WINDOW` (30) holding the FR5
    guarantee. Simulated over 40k draws against the real catalog: returns landing within 15
    draws of a tip becoming eligible again fell from **28.4% to 13.1%**, worst-case gap between
    showings of one tip from 831 draws to 640. Long-run usage evenness barely moved, because
    uniform was already even in aggregate — the clustering was always in the gaps, not the
    totals, which is why the fix targets gaps.
  - **A shuffled bag was rejected**, along with per-group `VarietyLevel` settings. Reasons for
    both are written up in README "Notable design decisions" rather than only here.
  - **The lag suspicion is now bounded, not confirmed.** Measured on the desktop JVM against the
    real catalog: `messageFor` ~7µs per draw, `TipCatalog.loadDefault()` ~3-5ms. The selection
    algorithm is under 1% of just parsing the catalog, so it is ruled out as the cause by
    measurement rather than argument; the catalog-parse suspect survives but a desktop number
    is a bound, not evidence about the device. No instrumentation was added to `:app` for this —
    that would be shipping production logging for a hypothesis, and this session had no device.
  - PRIVACY.md updated for the longer history (90 tip texts, and explicitly: no timestamps, no
    counts, no record of whether anything was looked at). No UI or strings changed —
    "More variety" still describes the practical/tone lean accurately.
  - **Note on a concurrent pass**: the tip content files changed underneath this work
    mid-session (general 41→45, motivation 38→43, wellbeing 37→45) from the content pass logged
    directly above. Nothing here depends on pool sizes, and the ktlint violations that pass
    reported in `TipEngineTest.kt`/`TempSimulationTest.kt` were this work in flight —
    `TempSimulationTest.kt` was measurement scaffolding and has been deleted, and
    `ktlintCheck` is green now.

## Verified for real (not just reviewed)

- **The 2026-07-27 selection rework is build-verified**: `./gradlew ktlintCheck test lint build`
  all green against the scratch JDK 17 + Android SDK (see "Local environment notes" — both
  survived from the previous session and needed no re-setup). `:core` 71 tests, `:app` 12, 0
  failures; `lint` 0 errors and the same 17 pre-existing warnings as before, nothing new; the
  full `build` includes `assembleRelease` with R8. The behavioural claims in that entry (kind
  mix per day part, the 28.4% → 13.1% early-return figure, the µs/ms timings) come from a
  throwaway simulation harness run against the real catalog, which has since been deleted — the
  properties worth keeping are asserted in `TipEngineTest`/`ToneProfileTest` instead, since a
  measurement script nobody runs again is not a guarantee.
  **Not verified on a physical device**, and this session had no device or adb access. What
  specifically needs eyes on it, in rough priority order:
  1. **Night, between 23:00 and 05:59** — the biggest behaviour change a user would notice.
     Previously one fixed sentence for ~7 hours; now roughly half wind-down message, half quiet
     philosophy/wellbeing. Worth checking it reads as calm rather than as the app having lost
     the plot at 2am. Easiest via a temporarily-adjusted system clock.
  2. **Whether tone tips now feel timed rather than random** — the whole point of `ToneProfile`.
     This is a judgement no test can make: it needs someone seeing a motivational line in the
     morning and a wellbeing one in the evening and agreeing that's right.
  3. **Whether rotation still feels repetitive.** The recency weighting is aimed squarely at the
     original report, but the metric it improves (early returns) is a proxy for a feeling. If it
     still feels repetitive after a week of real use, the next suspect is the anti-repeat window
     itself rather than the weighting.
  4. **The tap-to-refresh lag** — unresolved, and now the only remaining open question from the
     brief besides the sleep-pool content. Needs logcat timing around process start and
     `TipCatalog.loadDefault()` on the device; the desktop numbers only rule the algorithm out.
- **The notifications-removal/tip-retiming/widget-refresh-race-fix pass above is verified**
  via `ktlintCheck`, `test` (`:core` 29 tests — down from 44 with `QuietHoursTest`/
  `NextOccurrenceTest` gone, plus the 3 new `TipRefreshScheduleTest` cases; `:app` 5 tests
  per variant/10 total, down from 10/20 with every notification-settings DataStore test gone
  — all green), `lint`
  (0 errors, 17 pre-existing dependency/icon warnings, same categories as before this pass,
  nothing new), and a full `build` including `assembleRelease` with R8 minification — all run
  against the same real JDK 17 + Android SDK scratch setup as the earlier passes (see "Local
  environment notes"). **Not yet verified on a physical device**: the retimed tip advancement
  (needs waiting out real ticks or manipulating device time to see it actually fire), the
  widget's tap-to-refresh action, and — most importantly — whether the widget-background
  race is actually gone now (the bug this pass targets was confirmed still happening
  intermittently by the user, so the fix needs a real re-test, not just a passing build) are
  all still pending an on-device pass. See "In progress right now."
- The DST/time-zone-safe-scheduling/concurrency/backup-rules/branding/API-36 pass above is
  verified via `ktlintCheck`, `lint` (0 errors), `test` (`:core` 44 tests including the new
  DST/timezone matrix and the mutex-regression concurrency test, `:app` 10 tests, all green),
  and a full `build` (including `assembleRelease` with R8 minification against `compileSdk`/
  `targetSdk 36`) — all run against a real JDK 17 + Android SDK, not just reviewed. The
  merged release manifest was read directly to confirm no `INTERNET` permission. **Not
  verified on a physical device**: none of this pass's behavior (DST transitions, time-zone
  changes, boot rescheduling, the backup-rules fix, the widget's branding label) has been
  exercised on-device — DST/clock-change/boot scenarios in particular are hard to fully
  cover any other way; see the manual test plan in this session's summary.
- The notifications-master-switch/tip-refresh/settings-restructure pass above is verified via
  `compileDebugKotlin`, `testDebugUnitTest` (including a new `setNotificationsEnabled`
  DataStore test), and `ktlintCheck` — all green, and the build has since been installed on
  the device. **Not yet confirmed on-screen**: the collapsible About row, the Notifications
  card's collapse-on-disable, the refresh-tip button's visible effect on the widget, and the
  four re-angled/recolored backgrounds have only been checked as compiled resources, not as
  rendered pixels — pending the user's next pass through the app.
- `:core` — 37 unit tests pass (JVM, no Android needed) — includes a new `TipCatalogTest`
  case for the citation requirement.
- `:app` — 10 unit tests pass, debug and release variants.
- `ktlintCheck`, `lint` (0 errors), full `build` including `assembleRelease` with R8
  minification — all green, after fixing the `Tip`-typed API in every test that still
  expected raw `String`s (`TipEngineTest`, `AdvanceTipUseCaseTest`, `TipCatalogTest`).
- **CI is live and green**: GitHub Actions ran on push `b192ea8` and passed — see the badge
  in README.md or the [Actions tab](https://github.com/alexanderkova033/health-widget/actions).
  Everything described in this file is **local and uncommitted as of this writing** — CI
  hasn't seen any of the settings-redesign/citation/anti-repeat-window work yet.
- On-device: the widget-style picker bug (see above) was reproduced and confirmed exactly
  as reported (both the original "stuck after one change" version and the follow-up "lags,
  doesn't change" regression from the first fix attempt). The current (second) fix is
  installed on the device but **not yet re-verified on-screen** — see "In progress right
  now." Before either bug was found, this same build's settings screen (icons, trimmed
  text, Card sections, live widget preview, style swatches, frequency slider to 6, the
  "100% offline" string) was confirmed rendering correctly via `adb` screenshots, and the
  notification-permission flow, WorkManager job scheduling, and settings
  persistence-through-reinstall all checked out via `adb`/`dumpsys` in an earlier pass this
  session.

## Release readiness

- **Signing**: `keystore/healthwidget-release.jks` exists locally (gitignored — not in the
  repo). `app/build.gradle.kts` applies it to the `release` build type when
  `keystore/keystore.properties` is present; falls back to unsigned when absent (so CI
  isn't affected). **The passwords in `keystore/keystore.properties` are only on this
  machine — back them up externally (password manager) before this machine's disk is the
  only copy.** See `keystore/README.md`.
- **Store icon**: `store-assets/play-store-icon-512.png` — flattened 512×512 PNG for the
  Play Console listing.
- **Not yet done**:
  - **Naming is decided (2026-07-24): the app stays "HealthWidget," `applicationId` stays
    `com.healthwidget.app`.** Nothing to change here before the first Play Store upload — the
    three alternate names previously under consideration (Quiet Cue / MicroPause / Driftwell)
    are dropped.
  - No Play Console developer account yet ($25, identity verification).
  - `PRIVACY.md` isn't hosted at a public URL yet (Play Console requires one — GitHub Pages
    on this repo is the easy option).
  - No screenshots (needs the app running on a device).
  - No signed `.aab` has been uploaded anywhere yet — `bundleRelease` produces
    `app/build/outputs/bundle/release/app-release.aab` locally, gitignored, not committed.

## In progress right now

The tap-to-refresh-sleep-hours-fix/tip-linked-background pass (see above) is code-complete
and build-verified (`ktlintCheck`, `test`, `lint` 0 errors, full `build` including
`assembleRelease`), but **not yet exercised on the physical device** — this session, like
the one before it, did not have device/adb access. Things that specifically need a real
on-device pass before this can be considered done, in rough priority order:

1. **The tap-to-refresh fix** — confirm tapping the widget card changes the tip during
   ordinary daytime hours (should have always worked) *and* during 23:00-05:59 (the bug just
   fixed; easiest to check by temporarily setting the device clock into that window, then
   reverting it).
2. **The tip-linked background** — confirm a tip change (via tap, the Settings refresh
   button, or the periodic worker) visibly changes the card's background style, and that it
   reads as an intentional refresh rather than a jarring flicker.
3. Everything carried over unverified from the previous pass below is still open: the widget-
   background race fix, the retimed ~90-minute tip advancement, and the original tap-to-
   refresh/gear-icon shortcut.

Two methodological notes for next time:
- While both this session and the user were sending input to the same phone at once (adb
  `input tap` here, real touches from the user), the two interleaved and produced confusing,
  hard-to-interpret results (a style selection appeared to "jump back" to an earlier
  choice). Prefer read-only observation (screenshots, logcat, `dumpsys`) plus asking the
  user to perform the interaction, over sending synthetic input to a device the user is
  also actively holding.
- A fix that passes `ktlintCheck`/`test`/`lint`/`adb`-observed-once isn't necessarily the
  final fix — the WorkManager-based widget-refresh fix looked completely correct and was
  confirmed working via screenshots, but the user caught a UX regression (lag) that no
  automated check here would have surfaced. Real user re-testing after "it's fixed" claims
  matters even when the automated signals are all green.

## Local environment notes

This machine has **no JDK or Android SDK installed by default**. To verify builds, a JDK 17
(Temurin) was downloaded to `%TEMP%\claude\jdk17` and a minimal Android SDK to
`%TEMP%\claude\android-sdk-empty`, both outside the repo and both ephemeral (not guaranteed
to survive a reboot or session end) — as of the API 36 migration, that SDK dir also has
platform 36 and build-tools 35 (AGP auto-downloaded them on first build once licenses were
pre-accepted; no `sdkmanager`/cmdline-tools install was needed). A future session verifying
a build from scratch will need to either redo this or use a real Android Studio / SDK
installation.

Notes from repeated from-scratch JDK setups in this environment:
- A plain `unzip -q big.zip` has silently dropped most files with no error more than once
  (extracting only ~2 of ~500 expected files on one occasion) — always re-extract without
  `-q` and sanity-check the file count (e.g. `find <jdk-dir> -type f | wc -l`, expect several
  hundred) before trusting an extraction.
- This machine can carry **orphaned Gradle/Kotlin daemons from an earlier session** still
  running against the same scratch JDK path — if a fresh `rm -rf` of that JDK directory hits
  "device or resource busy" on `modules`/`jrt-fs.jar` specifically, that's usually why. Check
  `Get-Process java` (PowerShell) for processes rooted at the scratch JDK path before
  force-deleting; stop them first (equivalent to `gradlew --stop`) rather than fighting a
  live daemon's open file handles, which can otherwise leave a half-overwritten, broken JDK
  copy behind.

## Assumptions and deliberate decisions worth knowing

- Fixed nudge times per frequency level 0-6 (10:00/14:00/18:00 etc.), not user-configurable
  exact times — see `NudgeScheduler.kt`; product spec left this unspecified.
- The anti-repeat window and the length of the stored history are two separate constants as of
  the 2026-07-27 selection rework: `TipHistoryRepository.ANTI_REPEAT_WINDOW` (30) is the FR5
  guarantee, `MAX_RECENT_TIPS` (90) is how much is remembered so `TipEngine` has recency signal
  to weigh. Neither is tied to a pool size; the window comfortably fits under every day-part
  pool (~69 unique practical tips per day part, plus 124 tone tips), so the fallback-to-repeat
  path is rare in practice, not the common case.
- Sleep-hours wind-down messages are exempt from the anti-repeat rule by design (one fixed
  message per window, not a pool) — see `TipEngine.kt`. They are no longer the *only* thing
  shown at night, though: since the 2026-07-27 rework they're weighed against the philosophy and
  wellbeing pools, and are ~50% of night draws at the default variety level.
- No ViewModel, no DI framework (`AppContainer` is hand-written) — deliberate, the app is
  too small to justify either; see README "Notable design decisions."
- AGP is now 8.10.1 / `compileSdk`/`targetSdk 36` (previously 8.7.3 / 35 — an earlier attempt
  at other dependency bumps, e.g. activity-compose 1.13/core-ktx 1.19, had been reverted
  after cascading into requiring `compileSdk 36+`/AGP 9.x, which felt out of scope at the
  time; API 36 was later done deliberately, on its own, as its own task). AGP 9.x was
  considered and not used — 8.10.1 is the minimum AGP version that supports `compileSdk 36`,
  and jumping to a new major version wasn't judged necessary just to reach API 36.
