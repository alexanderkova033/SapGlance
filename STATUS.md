# Project status

_Last updated: 2026-08-02_

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
- **Catalog**: 385 tips — 182 practical (`general` 58, `morning` 31, `afternoon` 31, `evening`
  33, `sleep_late` 15, `sleep_early` 14) and 203 tone (`motivation` 67, `philosophy` 65,
  `wellbeing` 71). Every practical line still carries 2+ independent citations, and the pass on
  2026-07-31 reworded most of them without changing a single claim (see below). The 2026-08-02
  pass added ten quoted lines and no practical ones: six to `philosophy`, closing the African,
  indigenous and last-hundred-years gaps its header had named, and four jokes to `wellbeing`
  from three voices it did not have.
- **Languages**: `en` and `ru`, 770 tip lines in total. English lives at the root of `tips/`
  and is the text the citations were checked against; Russian is a translation of it at
  `tips/ru/`. **Citations are shared, not translated** — one `_sources.txt` per pool, zipped by
  position against both languages, so a translation that gains or loses a line fails at load.
- **Choosing a language**: an in-app picker (`TipLanguage`: System / English / Русский) in
  Settings, next to the variety control, plus Android 13+'s own per-app picker via
  `localeConfig`. The in-app one is the load-bearing half: it is the only one that exists below
  API 33, and it is what decides which catalog the *widget* reads. The setting drives both the
  tip catalog and the settings screen's own strings, so the picker can show you the language it
  is about to switch to.
- **Widget**: 19 background styles, picked by a per-hour palette narrowed by the tip's kind and
  then hashed on the tip's text; resizes from 2x2 to 4x4.
- **Selection**: three narrowing weighted picks, anti-repeat applied before all of them,
  recency weighting over a 160-tip history, per-day-part `ToneProfile`, no tone voice three
  draws running.
- **Schedule**: the tip changes **four times a day, at 06:00, 12:00, 18:00 and 23:00**, which are
  `DayPart` boundaries — so the line on screen always belongs to the part of the day you are in.
  Replaced the "~90 minutes of confirmed screen-on time" rule on 2026-07-31; 23:00 was added on
  2026-08-02. The worker compares a persisted slot rather than counting down, so a missed tick, a
  Doze window or a dead process costs nothing and a whole missed day still produces one advance
  rather than a burst.
- **Choosing what you get**: one control per pool (health / philosophy / motivation / wellbeing),
  each at none, some or plenty. Replaced a single three-position lean on 2026-08-02, because that
  control moved all three tone voices together and they are not one thing. Health sets how much
  tone there is at all; the other three divide that share between themselves. **"None" means
  none**, which is a deliberate break with the old "a lean, never a filter" promise — and the
  reason FR5 is now conditional, see below. The last pool standing cannot be switched off.
- **Build gate**: `ktlintCheck` clean; `:core` 150 tests, `:app` 12 per variant, 0 failures;
  `lint` 0 errors, 38 warnings; full `build` including `assembleRelease` with R8.

`:core` went 102 tests to 125 when `TipCatalogTest` was parameterized over the supported
languages. That is the same invariants run twice, not new ones: a translation that drops a line,
repeats one or smuggles in an em dash is exactly as broken as an English pool that does, and
more likely, since the translator is working against a file whose sources they cannot read. The
nine after that came with the language setting, and the one worth knowing about is
`concurrent advances across two languages still serialize against one lock` — see
`AdvanceTipUseCase`'s class doc for the refactor it exists to stop. The 147th arrived with the
23:00 switch: `TipRefreshScheduleTest` gained a case for the new window, and the test that used
to pin the night pools as tap-only was rewritten rather than deleted, because half of what it
pinned is still true. See item 15.

The lint warnings are `VectorRaster` 16, `GradleDependency` 8, `UnusedAttribute` 5,
`UnusedResources` 3, `MonochromeLauncherIcon` 2, `AndroidGradlePluginVersion` 2, `VectorPath` 1,
`UseKtx` 1. The `UnusedAttribute` five are deliberate: the widget's API 31+ `targetCell*` and
`maxResize*`, and the manifest's API 33+ `localeConfig`, all against `minSdk 26`.

**The count read 35 here and now reads 38, and none of the three is from the content work** — it
touches no Android resource and no manifest. They are `R.color.widget_linen_fold_back`,
`widget_linen_fold_front` and `widget_lilac_veil_front`, defined in `colors.xml` and referenced
by nothing, left behind when three backgrounds were rebuilt on 2026-07-31. `widget_lilac_veil_back`
*is* used, which is what makes the other three look like leftovers rather than a naming mistake.
Harmless, and worth clearing next time that file is open. The earlier drift, from 26, is the
version-age checks noticing the calendar.

**One lint check earned its keep and is worth recording.** `AppBundleLocaleChanges` fired when
the in-app language picker landed, and it was right: Play splits an App Bundle's resources by
device locale, so a reader on an English phone would have been offered Russian and handed a
settings screen whose Russian strings were never delivered. Worse than an obvious break, because
the *tips* would have switched anyway — they are `:core` JVM resources, which are not split. The
fix is `bundle { language { enableSplit = false } }` in `app/build.gradle.kts`, which costs 27
strings of APK and is the reason that warning is no longer in the list above.

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
   385. The 89 lines added up to 2026-07-31 were checked against it by width rather than
   re-measured, this time by wrapping the whole catalog greedily at every column width with an
   approximate serif-bold advance table and ranking the result. That is a *relative* answer, not a
   measurement — the units are arbitrary — but it is the right relative answer: the
   widest-wrapping new line ("Light sensitivity varies fifty-fold between people") sits below
   ten lines that were already in the measured set, and the line the constant was measured
   against ("Morning movement outdoors does double duty") is unchanged. So the constant still
   covers the catalog, and nothing has re-run the wrap against the real font.
   The ten added on 2026-08-02 got less than that: the longest is 77 characters, comfortably
   inside the range already swept, and their widest unbreakable tokens ("intelligently",
   "подкрепиться") are shorter than the worst cases both languages were screened against
   ("procrastination", "Многозадачность"). That is a check that nothing got *worse*, which is
   weaker than the screening above and much weaker than a measurement.
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
7. **No new background has ever been seen on a phone, and it has already cost something.**
   Eight were drawn blind, judged "almost the same and quite boring" on sight, and the source
   confirmed it: every path in all eight started at the bottom-left corner, so they were one
   composition in eight colours. Three were rebuilt around different compositions and the
   contrast was raised, still blind. **Stop adding styles until some have been rendered** —
   this is the second round of work that eyes on a screen would have prevented, and the
   remaining questions (does Slate read as restful or unfinished, does Lilac's halo survive the
   0.30 white scrim, are five bottom-band cards too many) are all questions only looking answers.
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
9. **Whether the jokes land.** The group went four to seven on 2026-07-31 and seven to eleven on
   2026-08-02, both times on the header's own test ("does it still sound like it belongs beside
   the plant?"), which is one person's ear and nothing more. Warner was the first risky bet:
   gardening humour from 1870 is warm, and it is also 156 years old, which is a different thing
   from funny. The three voices added since are each a different bet. Twain and Leacock are both
   jokes at this app's own expense — one about the trouble of making maxims, on a widget that
   shows maxims; one from eight pages mocking people who optimise their health, printed inside an
   app that suggests health habits — and self-mockery either reads as honest or as a widget
   undercutting itself, which is not a thing a test can settle. Milne is the opposite risk: two
   lines that are unambiguously warm and might be too soft to be funny at all. Counts are the rule
   here — Jerome three, Warner two, Milne two, and one each of Wilde, Bierce, Twain and Leacock.
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
11. **The Russian has been passed over once, and one reader's ear is the only thing that says it
    is better.** The native reader's verdict on 2026-08-02 was that the whole catalog read as
    unintentionally funny. All nine pools were reworked on 08-02 and 08-03, and the causes turned
    out to be four recurring kinds rather than a scatter of weak lines. All four are written into
    `ru/general.txt`'s header, and the reach of each is worth knowing:

    - **Second-person past tense is gendered in Russian**, so "если долго печатал" told a woman
      the app assumes she is a man. A correctness bug rather than a style note. It was in
      `general`, `afternoon`, `wellbeing`, `philosophy` and `motivation` — five pools, and the
      first grep for it missed most of them because the word list was a guess. The catalog is
      now clean by a check that tokenises every past-tense form rather than looking for
      suspects.
    - **Calqued idiom.** "Uncross your legs" was "Расставь ноги", which means the opposite.
    - **False friends of jargon.** "пикует" for "peaks", "ядро тела" for core temperature,
      "устойчивость к инсулину" where the term is инсулинорезистентность.
    - **Calqued abstractions.** "менее реактивно", "плоское настроение", "хорошую вещь".

    **What this pass is not:** a second opinion. The same person who wrote the bad version wrote
    the fix, against rules derived from one round of feedback, and the practical pools got
    surgery rather than a rewrite on the argument that numbers and mechanisms carry less idiom —
    which is a judgement, not a measurement. `motivation` is the one pool rebuilt line by line.
    Still open for a native ear: whether it pushes without barking, whether `wellbeing` stays
    warm without turning sympathetic, and whether the jokes land — Milne especially, where
    Russian readers hear Заходер and these lines deliberately say "Пух" rather than "Винни-Пух"
    (reasoning in `ru/wellbeing.txt`'s header).
12. **Whether English citations under Russian tips are acceptable or merely defensible.** By
    design: the study is in English and translating a journal's name would make the citation
    harder to check, not easier. It is still a Russian reader tapping "почему этот совет?" and
    getting a wall of English. Nobody has watched that happen.
13. **Nothing at all has been seen on a device since 2026-07-31 morning.** The phone
    disconnected during a temp-directory cleanup and everything since is argued rather than
    observed: the language toggle, the Russian catalog, the rebuilt backgrounds, the schedule,
    the 23:00 switch, and the ten tips added on 2026-08-02. This is now four passes of work
    deep, which is the thing to fix before a fifth is added on top of it.
14. **The new schedule has never run through a real day.** The slot logic is pinned by tests at
    every boundary and across a full simulated day, and the tests are the easy half. What they
    cannot show is the thing that actually matters: that WorkManager fires within 15 minutes of
    06:00 on a phone in Doze, that the switch lands before the reader picks the phone up, and
    that four tips a day reads as a rhythm rather than as a widget that has stopped working.
    The first morning after installing is the whole test. **23:00 is the harder half of it**, and
    for a reason no test reaches: it is the one switch that fires while the phone is most likely
    to be idle, face down and deep in Doze, so a 15-minute tick is least likely to be prompt
    exactly where lateness is most visible. A reader who looks at 23:05 and sees the evening tip
    has no way to tell a deferred switch from a broken one.
15. ~~**The night pools are now unreachable except by tapping.**~~ Half closed 2026-08-02: `23`
    was added to `TIP_SWITCH_HOURS`, so a fourth switch opens `sleep_late` and someone awake at
    midnight gets a line written for the hour instead of an evening one that has been up for six
    hours. **`sleep_early` (00:00-05:59) is still tap-only** — midnight is the fifth `DayPart`
    boundary and is deliberately not a switch, since the 23:00 tip is written for the same reader
    in the same state and swapping it for another night tip at 3am buys nothing anyone is awake
    to see. 14 tips reachable only by tapping, down from 29, and a test pins it so the decision
    cannot rot into an oversight. What is genuinely unverified is smaller and is in item 14: no
    23:00 switch has ever fired on the phone.
16. **The language toggle has never been tapped on a device.** Built and shipped into the
    release APK on 2026-07-31, but the phone was disconnected before it could be installed, so
    everything below is argued rather than seen. Three things are worth checking first, in this
    order. Whether the settings screen actually re-renders in the chosen language, which rests on
    overriding `LocalContext` *and* `LocalConfiguration` together and would look like a dead
    toggle if only one took. Whether the widget follows without a process restart, which rests on
    the language being *observed* inside `provideContent` rather than captured above it, and
    which would show up as tips in the old language until something killed the app. And whether
    the tip history reset reads as intended or as a bug, since switching leaves the reader
    mid-rotation in a language they have seen none of.

17. **The per-pool control has never been touched on a device**, and two things about it are
    argued rather than seen. Four pools each with three chips is a much taller card than the one
    row it replaced, and it is being read at font scale 1.1 with One UI's own widening on top —
    the label sits above its chips rather than beside them for exactly that reason, and nobody has
    checked whether the card is now too tall to take in. And the disabled "none" on the last pool
    left on has never been looked at: it is meant to read as a rule, and a greyed chip that a tap
    does nothing to can just as easily read as a broken one.
18. **A reader can now narrow themselves below the anti-repeat window, and FR5 no longer covers
    that.** Switch off three pools and the fourth may hold fewer than 100 tips — philosophy alone
    is 65 — at which point the window cannot be honoured and the engine repeats. It degrades
    rather than failing, it keeps drawing from the pool the reader actually asked for rather than
    quietly reintroducing one they switched off, and both halves are pinned by tests. What is
    unverified is whether it *reads* as a consequence of their own setting or as the app breaking.

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
  were added. That header's next debt — nothing African, nothing indigenous, nothing from the
  last hundred years — was paid on 2026-08-02 with Ptahhotep, an Akan proverb, two Ohiyesa lines
  and Russell. What is still open there is narrower and is written into the header: three women
  out of everyone in the pool, and a last hundred years that is one book deep.
- **The catalog is 65 tips larger than anything that has been read end to end.** 55 were added on
  2026-07-31 and ten more on 08-02. The practical ones lean on citations already verified in this
  repo rather than on new ones, which is the safer half of the job — the claim still has to be one
  those sources actually make. Two were written to sit deliberately close to a line that was
  already there ("walking breaks beat standing breaks" next to "break up long sitting with a
  minute of standing"), and close is where a contradiction hides. Worth a read-through when there
  is reason to touch the pools again.
- **The quoted lines added on 2026-08-02 were verified one way and not the other.** Each was
  pulled from the raw source text and matched character by character, which is the check that
  catches a paraphrase — and it caught one immediately, since Eastman's line circulates as "It
  *is* our belief" and the book says "It was". What that method cannot catch is a line that is
  quoted correctly and *chosen* badly: an aphorism that means something else in its own
  paragraph. The Akan proverb is the one to re-read on that basis, because it is the only line in
  the pool that arrives through an ethnographer's gloss rather than from the author's own page.

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
  practical plus all of `philosophy` and `wellbeing` and none of `motivation` (its night
  `ToneProfile` weight is 0), which is 150-151 tips against a window of 100. That was ~110 before
  2026-07-31. The figure read 136-137 here until 2026-08-02 and was stale rather than wrong — it
  was computed mid-pass, before the last four philosophy lines of that day landed. The shape of
  the constraint has not changed and neither has the window.
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
