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
- **Catalog**: 371 tips — 182 practical (`general` 58, `morning` 31, `afternoon` 31, `evening`
  33, `sleep_late` 15, `sleep_early` 14) and 189 tone (`motivation` 67, `philosophy` 55,
  `wellbeing` 67). Every practical line still carries 2+ independent citations, and the pass on
  2026-07-31 reworded most of them without changing a single claim (see below).
- **Languages**: `en` and `ru`, 742 tip lines in total. English lives at the root of `tips/` and
  is the text the citations were checked against; Russian is a translation of it at `tips/ru/`.
  **Citations are shared, not translated** — one `_sources.txt` per pool, zipped by position
  against both languages, so a translation that gains or loses a line fails at load. The catalog
  is chosen once per process from `Locale.getDefault().language`, and Android 13+ shows SapGlance
  in the per-app language picker.
- **Widget**: 19 background styles, picked by a per-hour palette narrowed by the tip's kind and
  then hashed on the tip's text; resizes from 2x2 to 4x4.
- **Selection**: three narrowing weighted picks, anti-repeat applied before all of them,
  recency weighting over a 160-tip history, per-day-part `ToneProfile`, no tone voice three
  draws running.
- **Build gate**: `ktlintCheck` clean; `:core` 125 tests, `:app` 6 per variant, 0 failures;
  `lint` 0 errors, 35 warnings; full `build` including `assembleRelease` with R8.

`:core` went 102 tests to 125 when `TipCatalogTest` was parameterized over the supported
languages. That is the same invariants run twice, not new ones: a translation that drops a line,
repeats one or smuggles in an em dash is exactly as broken as an English pool that does, and
more likely, since the translator is working against a file whose sources they cannot read.

The lint warnings are all `VectorRaster`, `GradleDependency`, `AndroidGradlePluginVersion`,
`MonochromeLauncherIcon`, `UseKtx`, `VectorPath`, plus three deliberate `UnusedAttribute`: the
widget's API 31+ `targetCell*` and the manifest's API 33+ `localeConfig`, both against
`minSdk 26`. The count read 26 here until 2026-07-31 and now reads 35; 34 of those predate the
content work, which was confirmed by running `lint` with the change stashed, and the 35th is the
`localeConfig` attribute. The drift from 26 is the version-age checks noticing the calendar.

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
   371. The 89 lines added since were checked against it by width rather than re-measured, this
   time by wrapping the whole catalog greedily at every column width with an approximate
   serif-bold advance table and ranking the result. That is a *relative* answer, not a
   measurement — the units are arbitrary — but it is the right relative answer: the
   widest-wrapping new line ("Light sensitivity varies fifty-fold between people") sits below
   ten lines that were already in the measured set, and the line the constant was measured
   against ("Morning movement outdoors does double duty") is unchanged. So the constant still
   covers the catalog, and nothing has re-run the wrap against the real font.
2. **Whether the rotation feels varied.** Tests prove no tip repeats within 100 and no voice
   runs three deep. Whether it *feels* varied is a judgement no test makes.
3. **Whether the night hours read as calm** — now the open question about 29 tips rather
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
   been paid off twice over: eight new styles took the daylight palettes to ten and night to
   eight, and the card now refuses to draw the background it is replacing, so consecutive repeats
   are gone rather than merely rarer. What a screenshot still cannot settle is whether the
   *rotation* feels varied over weeks.
7. **The eight new backgrounds have never been seen on a phone.** Ember, Slate, Linen, Mist,
   Harbour, Canyon, Sage and Lilac are drawn but unrendered here: gradients and vector paths look different on-device than they
   do in the XML, and the one thing no test covers is whether the art survives the scrim it is
   drawn under. Slate in particular is deliberately almost empty, which is either restful or
   unfinished depending on how it actually lands.
8. **Whether the plain-English pass reads plainer.** 51 lines were reworded on 2026-07-31 — 44
   practical and 7 in `wellbeing`, none in `motivation` or `philosophy` — and 55 tips were added
   across all nine pools. The rewrites are argued from
   [docs/TIGHTENING_AUDIT.md](docs/TIGHTENING_AUDIT.md)'s five patterns, and every one of them
   was held to the same rule — no claim, no hedge and no number may change — so what the build
   can say is that nothing got less accurate. Whether "bedrooms sleep better cool" actually
   reads better than "a cooler bedroom supports better sleep" is the judgement, and it wants
   weeks of the widget on a wall rather than a diff. Two knock-on effects are certain and were
   taken knowingly: every reworded tip is a new string, so it is missing from stored history and
   is drawn on a different background than it used to be.
9. **Whether the three new jokes land.** The group went four to seven on the header's own test
   ("does it still sound like it belongs beside the plant?"), which is one person's ear and
   nothing more. Charles Dudley Warner is the new voice and the riskier bet: gardening humour
   from 1870 is warm, and it is also 156 years old, which is a different thing from funny. It
   stopped at seven rather than a round eight because an eighth would have had to be a fourth
   Jerome, and the counts are now the rule there — Jerome three, Warner two, Wilde one,
   Bierce one.
10. **The Russian has never been rendered on a phone, and one number says it might not fit.**
    `TipFace.minColumnRatio` was measured against English, and Cyrillic is not Latin: Russian
    words are longer and м, ш, ы, ю are wide. The screening described in item 1 was re-run with
    a Cyrillic advance table added, and it says no Russian line wraps wider than the widest
    English one. Two caveats, and the second is the honest one. The widest unbreakable Russian
    token (`Многозадачность`) is ~15% wider than the widest English one (`procrastination`),
    down from ~30% before ten lines were rephrased for exactly this reason. And **the screen
    disagrees with the on-device measurement about which English line is worst**, which means
    the approximate width table is not good enough to make an absolute claim from — it is a
    screen, not a measurement. The only thing that settles this is the Russian catalog on the
    Galaxy A34 with the per-app language set to Russian.
11. **Whether the Russian reads like Russian.** Written and checked by one person against the
    pools' own rules, which is enough to catch a lost hedge and not enough to catch what a
    native reader would wince at. Two specific worries. `motivation` must push without becoming
    the barking imperative Russian falls into. `wellbeing` must stay warm without sliding into
    the sympathetic register its first rule forbids, which Russian invites far more strongly
    than English does. Neither is something the build can see.
12. **Whether English citations under Russian tips are acceptable or merely defensible.** By
    design: the study is in English and translating a journal's name would make the citation
    harder to check, not easier. It is still a Russian reader tapping "почему этот совет?" and
    getting a wall of English. Nobody has watched that happen.

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
- ~~**The old `com.healthwidget.app` may still be installed on the test device.**~~ Closed
  2026-07-31: `pm list packages` on the A34 returns `com.sapglance.app` and nothing else, so the
  worry that a stale widget from the pre-rename app was still on the home screen is gone.
- **The tone pools were passed over on 2026-07-30** and the two criticisms recorded on 07-28 are
  closed; what each pass found, and what it left open, is written into the pool headers where a
  writer will actually look. The debt that pass left — German and Enlightenment rationalist on
  one line each, because it had spent its growth on Stoics — was paid on 07-31: two
  Schopenhauer, a second Spinoza, and Hume, who is a tradition the pool did not have. No Romans
  were added. What is still open there is narrower and is written into the header: the pool is
  European and Chinese apart from the Dhammapada, and has one woman in it.
- **The catalog is 17% larger than anything that has been read end to end.** 55 tips were added
  on 2026-07-31 and the practical ones lean on citations already verified in this repo rather
  than on new ones, which is the safer half of the job — the claim still has to be one those
  sources actually make. Two were written to sit deliberately close to a line that was already
  there ("walking breaks beat standing breaks" next to "break up long sitting with a minute of
  standing"), and close is where a contradiction hides. Worth a read-through when there is
  reason to touch the pools again.

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

**Checking the Russian on the device** without switching the whole phone over: Settings > Apps >
SapGlance > Language, which SapGlance appears in because the manifest declares
`android:localeConfig`. Two things to know before doing it. The app process restarts, which is
what makes the catalog pick up the change, and the widget needs a repaint to show it. And the
tip history resets, because history is keyed by tip text — so this is not a free check on a
device whose rotation you were using as evidence for anything else.

## Assumptions and decisions worth knowing

- **`applicationId` is permanent from the first Play upload.** It is `com.sapglance.app`. The
  display name can change freely.
- **The anti-repeat window is bounded by content, not preference.** 100 works because a
  single-day-part user's practical reach is 89-91 tips (76-78 before 2026-07-31) and an
  exhausted tier redistributes into tone rather than repeating; the cost is that user's
  practical share drifting below its nominal ~81%, by less than it used to now that the
  practical side is deeper, though nothing here has re-measured how much less.
  `TipCatalogTest` pins the promise itself. Raising the window needs more tips per pool, and the
  pool to count is still the night one, where the margin is thinnest: 23:00-05:59 reaches 14-15
  practical plus philosophy and wellbeing, 136-137 tips against a window of 100. That was ~110
  before 2026-07-31, so the margin roughly doubled. The shape of the constraint has not changed
  and neither has the window.
- **The sleep hours are ordinary pools now**, and the exemptions built around the fixed message
  they replaced are gone with it: no anti-repeat exemption, and no `manual` flag on
  `TipEngine.messageFor`, which existed only so a tap at 2am wasn't a silent no-op. They still
  take ~50% of night draws at the default variety level rather than the daytime 80%, but for
  new reasons — night reaches one practical pool rather than two, and a practical instruction is
  the least welcome register at 3am.
- Tips are identified by their **text** everywhere it matters, and that assumption survived
  going bilingual rather than being fixed by it. **Switching the phone's language resets the tip
  history**: the stored strings are in the old language, so `kindOf` answers null, anti-repeat
  matches nothing, and the reader starts a fresh rotation. That degrades correctly rather than
  crashing, and it is a reset, not a migration. Nobody who switches languages twice gets their
  first rotation back.
- **The language is read once per process**, in `AppContainer`, not threaded through selection.
  Safe because Android restarts the process when the system language changes, so a stale catalog
  cannot outlive the setting that chose it. If that ever stops being true, this is where it
  breaks.
- **`SUPPORTED_LANGUAGES` and `locales_config.xml` must agree, and nothing checks that.** One is
  Kotlin in a module with no Android on its classpath; the other is an Android resource. A
  language listed only in the XML offers the reader a translation that silently serves English
  tips.
- No ViewModel and no DI framework, both deliberate at this size.
- AGP 8.10.1, `compileSdk`/`targetSdk 36`. AGP 9.x skipped: 8.10.1 is the minimum for
  `compileSdk 36` and a major jump wasn't needed.
- **v1 has no notifications at all.** They were built then deliberately removed; any doc,
  comment or memory mentioning `NudgeScheduler` predates that and is wrong.
