# Project status

_Last updated: 2026-07-29_

A snapshot for picking the project back up: where things stand, what's actually been checked,
and what's open. **It is not a changelog** — the history of how the code got here is in
`git log`, which doesn't go stale. If this file and the repo disagree, the repo is right.

## Where it stands

Feature-complete for v1 and building clean. Not released, and not releasable yet — see
"Shipping" below, which is now the critical path rather than the code.

- **App**: SapGlance, `com.sapglance.app`, versionCode 1, versionName 0.1.0.
- **Modules**: `:core` (pure Kotlin domain) and `:app` (Android). Organised by feature.
- **Catalog**: 282 tips — 132 practical (`general` 50, `morning` 26, `afternoon` 26,
  `evening` 28, plus one fixed message for each of the two sleep windows) and 150 tone
  (`motivation` 53, `philosophy` 42, `wellbeing` 55).
- **Widget**: 11 background styles, tip-derived. Resizes from a 2x2 square to a 4x4 block.
- **Selection**: three narrowing weighted picks, anti-repeat applied before all of them,
  recency weighting over a 90-tip history, per-day-part `ToneProfile`, and a rule that no tone
  voice runs three draws in a row.
- **Build gate**: `ktlintCheck` clean, `:core` 77 tests and `:app` 6 per variant with 0
  failures, `lint` 0 errors and 26 warnings, full `build` including `assembleRelease` with R8.

The 26 lint warnings are all `VectorRaster`, `GradleDependency`, `MonochromeLauncherIcon`,
`UseKtx`, `VectorPath` and two `UnusedAttribute`. The last two are the widget's API 31+
`targetCellWidth`/`targetCellHeight` against `minSdk 26` and are deliberate — the XML says why.

## Verified, and not verified

**Verified by the build gate** (against a scratch JDK 17 + Android SDK — see "Local
environment"): everything in the list above. The `:core` behaviour claims are pinned by tests
rather than by intention, including the tone run limit, the `ToneProfile` night filter, and
the anti-repeat window reaching exactly the window rather than the whole history.

**Verified on a physical device** (Galaxy A34): the app installs and launches, the settings
screen renders and shows the SapGlance name, no crashes in logcat. Earlier passes confirmed
the settings UI, notification-permission flow, WorkManager scheduling, and
settings-persistence-through-reinstall.

**Not verified on a device, and this is the gap that matters:**

1. **The widget clipping fix and the resize range.** Both are dp arithmetic plus an estimate of
   serif character width. The worst case was checked per size bucket against a 90-character tip
   (2x2 needs 6 lines and has 6; every larger size has slack), but that's a calculation, not a
   screen. This defect was twice predicted absent before someone finally looked at a real phone,
   so it deserves more scepticism than usual.
2. **The tone run limit as felt behaviour.** Tests prove a third consecutive wellbeing line
   can't happen. Whether the rotation now *feels* varied is a judgement no test makes.
3. **Whether the night hours read as calm** rather than as the app having lost the plot at 2am.
4. **The tap-to-refresh lag.** Measured and understood (~1s is process start, not app code) but
   not re-felt since `warmUp()` landed.

Two methodological notes worth keeping:

- **Don't send synthetic input to a phone the user is holding.** A previous session's `adb
  input tap` interleaved with real touches and produced confusing, misread results. Prefer
  read-only observation (screenshots, logcat, `dumpsys`) and ask the user to do the tapping.
- **Green automated signals aren't the same as fixed.** The WorkManager widget-refresh fix
  passed every check and was confirmed by screenshot, and the user still caught a lag
  regression nothing here would have surfaced.

## Open defects and risks

- **The old `com.healthwidget.app` is still installed on the test device**, with its widget on
  the home screen. Because the rename changed the `applicationId`, SapGlance installed
  alongside it rather than replacing it. Anything looking wrong on that old widget is the old
  app's code, not a live bug.
- **The keystore is not backed up.** `keystore/healthwidget-release.jks` and the passwords in
  `keystore/keystore.properties` exist only on this machine. This is the one unrecoverable
  item in the project and it has been outstanding across several sessions.
- **Two content problems in the tone pools**, see "Planned next" below.

## Shipping: the Play Store path

**The binding constraint is not technical.** The developer is under 18, and a Play Console
account requires the holder to be 18+. It's in the Developer Distribution Agreement and is
enforced at signup and again by the payments profile, so there is no version of this where the
account is registered in their own name.

The only legitimate route is a **parent or guardian registering and owning the account**, with
the developer added as a Play Console user with release-management permissions. What the
guardian actually takes on: the $25 fee, an identity check, and their name and contact address
displayed publicly on the listing. What makes that an easy ask for this app specifically — no
data collection (structurally, via the absent `INTERNET` permission), no ads, no payments, no
accounts, no user content to moderate, and no medical claims.

Alternatives were considered and rejected on reach: F-Droid (the MIT licence qualifies, and it
needs no account or age check) and GitHub Releases + IzzyOnDroid both work, but their combined
audience is a rounding error next to Play, and reach is the goal. Worth revisiting only if the
guardian route is unavailable.

**Schedule driver**: a personal account created after Nov 2023 can't publish to production
until it has run a closed test with **12 testers opted in for 14 consecutive days**. The clock
starts when the 12th tester installs, so recruiting is the long pole — realistically three
weeks from first upload.

Ready:

- Signing config (`app/build.gradle.kts` applies the keystore when present, builds unsigned
  when absent, so CI is unaffected)
- `store-assets/play-store-icon-512.png`
- `store-assets/play-listing.md` — title, short description, full description, drafted against
  the quote-widget category rather than health trackers, plus the category recommendation
  (**Personalization**, which is where widget browsers look and avoids the Health apps
  declaration for an app holding no health permissions)
- `targetSdk 36` already clears the Aug 2026 requirement

Not ready:

- No Play Console account
- Keystore not backed up
- `PRIVACY.md` not hosted at a public URL (GitHub Pages is the easy option)
- No feature graphic (1024x500, required) and no screenshots
- `versionName` still `0.1.0`; worth `1.0.0` for a launch
- The widget size work is unverified on a device, and screenshots depend on it

## Planned next: the tone pools are too soft, and two of them overlap

User judgement, recorded 2026-07-28, from reading the pools in ordinary use. Not started. This
is a **content** problem, not a selection one — the run limit stops one voice repeating three
times over, but a rut of three is not why these two read the way they do.

**Philosophy is conventional rather than serious.** The sourcing discipline is real: every
quotation is checked verbatim against a public-domain edition, and the pool header lists the
misattributions it deliberately avoids. But accuracy is the bar it was built to clear, and
being *worth stopping on* is a different bar that was never aimed at. What comes out is the
philosophy everyone has already met, in the register of a poster. The likely cause is selecting
for quotability — a line short enough for a widget and instantly legible is usually one that has
been quoted to death. Worth trying: passages that need a beat to land, and ideas that cut
against the reader rather than reassure them. Note the tension with the ~90-character cap; "it
didn't fit" is how the pool got conventional in the first place.

**Motivation is nearly indistinguishable from wellbeing and needs to be stronger.**
`motivation.txt`'s own header says the pool "pushes" — short sentences, verbs up front, an
imperative where it fits — and explicitly banishes permission-giving lines to `wellbeing.txt`.
That rule has not held. The sharpest consequence: `ToneProfile` weights motivation 5/10 in the
morning and 0/10 at night, and that table only means anything if the two pools actually sound
different. If they don't, the time-of-day mechanism is moving weight between two names for the
same thing.

Nothing external constrains this pool — `TipKind.MOTIVATION` carries no citation burden, so how
hard it pushes is purely a choice. A pass should re-audit every line against the header's own
rule and move the failures into `wellbeing.txt` rather than softening the rule to match the
drift.

**A test would help and doesn't exist.** Nothing checks that motivation and wellbeing are
distinguishable, so the drift was invisible to CI and only surfaced by someone reading the
widget. Hard to assert well, but "no line in `motivation.txt` may lack an imperative verb" is
crude, mechanical, and would have caught most of it.

## Planned next: languages beyond English

Not started. The UI half is nearly free — every user-facing string is already in
`app/src/main/res/values/strings.xml` (the decorative `❝` glyph in `TipWidget.kt` aside), and
Glance reads the same resources as the settings screen, so a `values-<lang>/strings.xml` per
locale is all Android needs.

The content half is the actual project and isn't an Android-resources problem at all. The 282
tips live in `core/src/main/resources/tips/*.txt` and load through a classpath lookup that
knows nothing about `Locale`. Two structural obstacles:

- **Tips are identified by their text everywhere it matters** — history, `findByText`, and
  `WidgetStyle.forTip`'s hash. A wording edit already orphans a user's stored history; a second
  language multiplies that problem rather than adding to it.
- **Translation is not the hard part; re-sourcing is.** A practical tip carries citations to
  English-language primary literature, and a translated tip either keeps them (citing sources
  the reader can't read) or needs new ones. The philosophy pool is worse: its quotations are
  verified verbatim against specific public-domain editions, and a translation is either a
  different published translation with its own attribution or an uncheckable paraphrase.

Also note German and Russian typically run 20-30% longer than English, so translations will
clip where the English didn't — the widget's size range helps but doesn't remove this.

## Planned much later: iOS port

Not started, gated on hardware. `:core` is close to portable — `java.time.LocalTime` and
`TipCatalog`'s `getResourceAsStream` are the two JVM APIs a Kotlin Multiplatform build would
have to work around. The constraints that actually matter:

- **The refresh model doesn't port.** WidgetKit returns a *timeline* of future entries rendered
  against a daily budget; there is no background execution and no screen-state signal. iOS
  would get wall-clock rotation, which is exactly the model this project rejected because it
  rotates tips nobody saw. Decide deliberately rather than by default.
- **Tap-to-refresh needs iOS 17+.** Before that a widget tap can only deep-link into the app,
  which removes the primary interaction rather than degrading it.
- **Two processes instead of one.** The widget extension shares no address space with the app,
  so settings and history move to a shared App Group — and `AdvanceTipUseCase`'s `Mutex`, which
  serialises read-select-persist within one process, stops being sufficient.
- **The privacy promise doesn't translate literally, and it's the headline claim.** No iOS app
  can declaratively renounce network access. The truthful iOS version is weaker: no networking
  code, and an App Store label of "Data Not Collected". PRIVACY.md would need its own iOS
  section saying so plainly.
- **The 11 widget styles are Android vector drawables** and need rebuilding in SwiftUI. Lock
  Screen and StandBy widgets render monochrome, so the gradient card doesn't exist there at all.

**Open decision**: Kotlin Multiplatform `:core` versus a Swift reimplementation sharing only
the `tips/*.txt` content. KMP is the recommendation — selection is no longer trivial and
carries a real invariant, so a second hand-written implementation would be exactly the
divergence `AdvanceTipUseCase` exists to prevent, one platform up.

## Local environment

This machine has **no JDK or Android SDK installed by default**. Both were downloaded to
`%TEMP%\claude\` and are ephemeral — not guaranteed to survive a reboot.

- **Use `%TEMP%\claude\jdk17-new`** (or `jdk17b`). **`%TEMP%\claude\jdk17` is a broken partial
  extraction** — `java -version` works but it has no `release` file, so Gradle's toolchain
  detection refuses it with "no locally installed toolchains match".
- Gradle needs the toolchain pointed at explicitly, since nothing is on `PATH`:
  `./gradlew <task> -Porg.gradle.java.installations.paths=<jdk-dir>`.
- The Android SDK is at `%TEMP%\claude\android-sdk-empty`, with platform 36 and build-tools 35
  auto-downloaded by AGP on first build.
- `adb` is at `%TEMP%\claude\platform-tools\adb.exe`.
- **Don't pipe `adb exec-out screencap -p` through a PowerShell redirect** — it corrupts the
  binary with a BOM. Use `adb shell screencap -p /sdcard/x.png` then `adb pull`.
- A plain `unzip -q` has silently dropped most files here more than once. Check the file count
  before trusting an extraction — that's how the broken `jdk17` above happened.
- Orphaned Gradle/Kotlin daemons from earlier sessions can hold the scratch JDK's files open.
  Stop them (`gradlew --stop`) rather than force-deleting around a live daemon.

## Assumptions and decisions worth knowing

- **`applicationId` is permanent from the first Play upload onward.** It is `com.sapglance.app`
  and cannot change after that. The display name can change freely at any time.
- The anti-repeat window (30) and the stored history length (90) are two separate constants, and
  neither is tied to a pool size. The window fits comfortably under every day part's available
  pool (`general` 50 plus a day-part pool of 26-28, plus 150 tone tips), so the
  fallback-to-repeat path is rare rather than routine.
- Sleep-hours wind-down messages are exempt from anti-repeat by design — one fixed message per
  window, not a pool. They are ~50% of night draws at the default variety level, not 100%.
- Tips are identified by their **text** everywhere it matters. Fine while the app is
  single-language, and the main structural obstacle to shipping another one.
- No ViewModel and no DI framework, both deliberate for an app this size.
- AGP 8.10.1 with `compileSdk`/`targetSdk 36`. AGP 9.x was considered and skipped: 8.10.1 is the
  minimum that supports `compileSdk 36`, and a major version jump wasn't needed to get there.
- **v1 has no notifications at all.** They were built and then deliberately removed; if a doc,
  comment, or memory mentions `NudgeScheduler` or notification settings, it predates that
  removal and is wrong.
