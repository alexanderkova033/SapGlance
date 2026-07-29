# Project status

_Last updated: 2026-07-29_

Where things stand, what's actually been checked, and what's open. **Not a changelog** — how
the code got here is in `git log`. If this file and the repo disagree, the repo is right.

## Where it stands

Feature-complete for v1 and building clean. Not released; the blocker is the Play account, not
the code.

- **App**: SapGlance, `com.sapglance.app`, versionCode 1, versionName 0.1.0.
- **Modules**: `:core` (pure Kotlin domain) and `:app` (Android), organised by feature.
- **Catalog**: 282 tips — 132 practical (`general` 50, `morning` 26, `afternoon` 26, `evening`
  28, plus one fixed message per sleep window) and 150 tone (`motivation` 53, `philosophy` 42,
  `wellbeing` 55).
- **Widget**: 11 tip-derived background styles, resizing from 2x2 to 4x4.
- **Selection**: three narrowing weighted picks, anti-repeat applied before all of them,
  recency weighting over a 160-tip history, per-day-part `ToneProfile`, no tone voice three
  draws running.
- **Build gate**: `ktlintCheck` clean; `:core` 80 tests, `:app` 6 per variant, 0 failures;
  `lint` 0 errors, 26 warnings; full `build` including `assembleRelease` with R8.

The lint warnings are all `VectorRaster`, `GradleDependency`, `MonochromeLauncherIcon`, `UseKtx`,
`VectorPath`, plus two deliberate `UnusedAttribute` for the widget's API 31+ `targetCell*`
against `minSdk 26`.

## Verified, and not

**By the build gate**: everything above. The `:core` behaviour claims are pinned by tests, not
intention — including the tone run limit, the `ToneProfile` night filter, the anti-repeat window
reaching exactly the window, and the real catalog sustaining that window in every day part.

**On a physical device** (Galaxy A34, release build): installs and launches, no crashes, widget
renders and repaints. The launcher reports its 2x2 card as **187x226dp** — worth knowing, since
that is not the ~154x183 generic sizing tables suggest, and the layout thresholds are tuned
against real numbers because of it.

**Not verified:**

1. **Widget layout at sizes other than 187x226dp.** The rest of the 110-320dp range is
   arithmetic, checked by sweeping the range in a simulation, not by looking. The font-size
   estimate rests on `EFFECTIVE_CHAR_WIDTH_RATIO`, measured once against a *serif* render that
   the card no longer uses — it is now conservative, which wastes space but cannot clip.
2. **Whether the rotation feels varied.** Tests prove no tip repeats within 100 and no voice
   runs three deep. Whether it *feels* varied is a judgement no test makes.
3. **Whether the night hours read as calm.**
4. **Tap-to-refresh lag** since `warmUp()` landed (~1s is process start, not app code).

Two methodological notes:

- **Don't send synthetic input to a phone the user is holding.** `adb input tap` once
  interleaved with real touches and produced misread results. Prefer read-only observation and
  ask the user to tap.
- **Green automated signals aren't the same as fixed.** The widget-refresh fix passed every
  check and was confirmed by screenshot; the user still caught a lag regression nothing here
  would have surfaced.

## Open risks

- **The keystore is not backed up.** `keystore/healthwidget-release.jks` and the passwords in
  `keystore/keystore.properties` exist only on this machine. The one unrecoverable item in the
  project, outstanding across several sessions.
- **The old `com.healthwidget.app` may still be installed on the test device** with its own
  widget on the home screen — the rename changed the `applicationId`, so SapGlance installed
  alongside rather than replacing it. Anything wrong on *that* widget is the old app's code.
  Unconfirmed since the device was last connected.
- **Content problems in the tone pools** — see the README roadmap.

## Shipping: the Play Store path

**The binding constraint isn't technical.** The developer is under 18; a Play Console account
requires the holder to be 18+, enforced at signup and again by the payments profile. The only
legitimate route is a **parent or guardian owning the account**, with the developer added as a
user with release permissions. That means the $25 fee, an identity check, and the guardian's
name and address shown publicly on the listing. What makes it an easy ask here: no data
collection (structurally — no `INTERNET` permission), no ads, no payments, no accounts, no user
content, no medical claims.

F-Droid and GitHub Releases were considered and rejected on reach, and are worth revisiting only
if the guardian route fails.

**Schedule driver**: a personal account created after Nov 2023 must run a closed test with **12
testers opted in for 14 consecutive days** before production. The clock starts when the 12th
tester installs, so recruiting is the long pole — realistically three weeks from first upload.

| Ready | Not ready |
| --- | --- |
| Signing config (keystore applied when present, unsigned when absent, so CI is unaffected) | No Play Console account |
| `store-assets/play-store-icon-512.png` | Keystore not backed up |
| `store-assets/play-listing.md`, drafted against the quote-widget category, recommending **Personalization** | `PRIVACY.md` not hosted at a public URL |
| `targetSdk 36` clears the Aug 2026 requirement | No feature graphic (1024x500) or screenshots |
| | `versionName` still `0.1.0`; worth `1.0.0` for launch |

## Local environment

**No JDK or Android SDK installed by default.** Both live in `%TEMP%\claude\` and are ephemeral.

- **JDK: use `%TEMP%\claude\jdk17b\jdk-17.0.19+10`.** Several JDK folders exist side by side;
  `jdk17` is a broken partial extraction with no `javac.exe`. Check with
  `find <dir> -name javac.exe` before trusting one. Setting `JAVA_HOME` is enough — no need for
  `-Porg.gradle.java.installations.paths`.
- **Android SDK: `%TEMP%\claude\android-sdk-empty`**, pointed at by a gitignored
  `local.properties`. AGP auto-downloads platform 36 and build-tools on first build.
- **adb: `%TEMP%\claude\android-sdk-empty\platform-tools\adb.exe`** — not on `PATH`.
- **Screenshots**: `adb exec-out screencap -p > out.png` works from **Bash**. Do not do it
  through a *PowerShell* redirect, which corrupts the binary with a BOM.
- A plain `unzip -q` has silently dropped most files here more than once — check the file count
  before trusting an extraction. That is how the broken `jdk17` happened.
- Stop orphaned daemons with `gradlew --stop` rather than force-deleting around a live one.

**Syncing to the phone**: the device runs a *release* build and there is no debug
`applicationIdSuffix`, so a debug APK fails on signature mismatch and the only way through is an
uninstall — which wipes the tip history. Always `assembleRelease` + `adb install -r`.

## Assumptions and decisions worth knowing

- **`applicationId` is permanent from the first Play upload.** It is `com.sapglance.app`. The
  display name can change freely.
- **The anti-repeat window is bounded by content, not preference.** 100 works because a
  single-day-part user's practical reach is 76-78 tips and an exhausted tier redistributes into
  tone rather than repeating; the cost is that user's practical share drifting ~81% to ~73%.
  `TipCatalogTest` pins it. Raising it needs more tips per pool first.
- Sleep-hours wind-down messages are exempt from anti-repeat by design — one fixed message, not
  a pool. They are ~50% of night draws at the default variety level, not 100%.
- Tips are identified by their **text** everywhere it matters. Fine while single-language, and
  the main structural obstacle to shipping another.
- No ViewModel and no DI framework, both deliberate at this size.
- AGP 8.10.1, `compileSdk`/`targetSdk 36`. AGP 9.x skipped: 8.10.1 is the minimum for
  `compileSdk 36` and a major jump wasn't needed.
- **v1 has no notifications at all.** They were built then deliberately removed; any doc,
  comment or memory mentioning `NudgeScheduler` predates that and is wrong.
