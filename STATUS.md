# Project status

_Last updated: 2026-07-31_

What has actually been checked and what has not, plus the risks and the assumptions worth
knowing. **Not a changelog** — how the code got here is in `git log`. **Not a plan** — where it
is going, and what is blocking release, is in [ROADMAP.md](ROADMAP.md). If this file and the
repo disagree, the repo is right.

## Where it stands

Feature-complete for v1 and building clean. Not released; the blocker is the Play account, not
the code.

- **App**: SapGlance, `com.sapglance.app`, versionCode 1, versionName 1.0.0.
- **Modules**: `:core` (pure Kotlin domain) and `:app` (Android). Same three features in both
  (`tips`/`settings`/`widget`). `:core` is flat per feature; `:app` splits each into
  `data`/`presentation`/`framework`, which are real differences in kind.
- **Catalog**: 316 tips — 151 practical (`general` 50, `morning` 26, `afternoon` 26, `evening`
  28, `sleep_late` 11, `sleep_early` 10) and 165 tone (`motivation` 59, `philosophy` 47,
  `wellbeing` 59).
- **Widget**: 15 background styles, picked by a per-hour palette narrowed by the tip's kind and
  then hashed on the tip's text; resizes from 2x2 to 4x4.
- **Selection**: three narrowing weighted picks, anti-repeat applied before all of them,
  recency weighting over a 160-tip history, per-day-part `ToneProfile`, no tone voice three
  draws running.
- **Build gate**: `ktlintCheck` clean; `:core` 102 tests, `:app` 6 per variant, 0 failures;
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
   `TipFace.minColumnRatio` was measured over the catalog at 282 tips and the catalog is now
   316. The 34 lines added since were checked against it by width rather than re-measured: the
   widest of them is narrower than three lines that were already in the measured set, so the
   constant still covers the catalog, but nothing has re-run the wrap.
2. **Whether the rotation feels varied.** Tests prove no tip repeats within 100 and no voice
   runs three deep. Whether it *feels* varied is a judgement no test makes.
3. **Whether the night hours read as calm** — now the open question about 21 new tips rather
   than about two fixed sentences. The pools were written to be gentler than the daytime ones
   and to leave out the frightening findings on purpose, but nobody has yet been awake at 3am
   with the widget in front of them, which is the only test that counts.
4. **Whether the reworked tone pools read better.** Both passes were argued from the pool rules
   and are recorded in the headers; whether philosophy is now worth stopping on, and whether
   motivation pushes, is a judgement the build cannot make.
5. **Tap-to-refresh lag** since `warmUp()` landed (~1s is process start, not app code).
6. **Whether the palettes read right over time.** Both halves are now confirmed on device: a
   philosophy line at 01:43 drew a deep card, and a motivation line at 15:30 drew Meadow, pale,
   with correctly paired ink — which is the morning palette that motivation borrows, at an
   afternoon hour, exactly as designed. The variety cost that narrowing introduced has since
   been paid off twice over: four new styles took the daylight palettes to eight and night to
   six, and the card now refuses to draw the background it is replacing, so consecutive repeats
   are gone rather than merely rarer. What a screenshot still cannot settle is whether the
   *rotation* feels varied over weeks.
7. **The four new backgrounds have never been seen on a phone.** Ember, Slate, Linen and Mist
   are drawn but unrendered here: gradients and vector paths look different on-device than they
   do in the XML, and the one thing no test covers is whether the art survives the scrim it is
   drawn under. Slate in particular is deliberately almost empty, which is either restful or
   unfinished depending on how it actually lands.

Two methodological notes:

- **Don't send synthetic input to a phone the user is holding.** `adb input tap` once
  interleaved with real touches and produced misread results. Prefer read-only observation and
  ask the user to tap.
- **Green automated signals aren't the same as fixed.** The widget-refresh fix passed every
  check and was confirmed by screenshot; the user still caught a lag regression nothing here
  would have surfaced.

## Open risks

- **The keystore backup is reported done but unverified.** Both files were copied off this
  machine on 2026-07-31. Nothing here can check that, and a backup that has never been opened is
  a belief rather than a backup: run `keytool -list -v -keystore <the copy>` against it once,
  with the password from `keystore.properties`, and the risk is genuinely closed.
- **The old `com.healthwidget.app` may still be installed on the test device** with its own
  widget on the home screen — the rename changed the `applicationId`, so SapGlance installed
  alongside rather than replacing it. Anything wrong on *that* widget is the old app's code.
  Unconfirmed since the device was last connected.
- **The tone pools were passed over on 2026-07-30** and the two criticisms recorded on 07-28 are
  closed; what each pass found, and what it left open, is written into the pool headers where a
  writer will actually look. The one thing worth repeating here: the philosophy pass replaced
  clichés within their own traditions but spent its net growth on Stoics, so German and
  Enlightenment rationalist still carry one line each. The next pass there should add a voice.

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
  `TipCatalogTest` pins it. Raising it needs more tips per pool first — and the pool to count is
  now the night one: 23:00-05:59 reaches ~11 practical plus philosophy and wellbeing, about 110
  tips against a window of 100, which is the whole margin the app has anywhere.
- **The sleep hours are ordinary pools now**, and the exemptions built around the fixed message
  they replaced are gone with it: no anti-repeat exemption, and no `manual` flag on
  `TipEngine.messageFor`, which existed only so a tap at 2am wasn't a silent no-op. They still
  take ~50% of night draws at the default variety level rather than the daytime 80%, but for
  new reasons — night reaches one practical pool rather than two, and a practical instruction is
  the least welcome register at 3am.
- Tips are identified by their **text** everywhere it matters. Fine while single-language, and
  the main structural obstacle to shipping another.
- No ViewModel and no DI framework, both deliberate at this size.
- AGP 8.10.1, `compileSdk`/`targetSdk 36`. AGP 9.x skipped: 8.10.1 is the minimum for
  `compileSdk 36` and a major jump wasn't needed.
- **v1 has no notifications at all.** They were built then deliberately removed; any doc,
  comment or memory mentioning `NudgeScheduler` predates that and is wrong.
