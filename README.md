# SapGlance

![CI](https://github.com/alexanderkova033/SapGlance/actions/workflows/ci.yml/badge.svg)

A privacy-first Android wellness widget. One home-screen card showing one rotating tip —
practical advice with real citations, or a line of motivation, philosophy or wellbeing.
No accounts, no tracking, no streaks, no notifications.

## The privacy promise

**100% offline, zero data collected.** No server, no analytics, no crash reporter, no ad SDK —
and the manifest declares no `INTERNET` permission, so a compromised dependency couldn't phone
home even if it tried. That's structural rather than a policy.

Tip history lives in on-device DataStore and rides along in Android's encrypted device backup
like any other app's local data. That's the one path off the phone, and [PRIVACY.md](PRIVACY.md)
says so plainly rather than glossing over it.

## What it does

- **A Glance widget** showing the current tip. Tap the card for a new one; the gear opens
  settings. (AppWidgets can't intercept long-press, so a dedicated target is the only way in.)
- **The tip advances once it's had a chance to be seen** — roughly 90 minutes of confirmed
  screen-on time, not a wall-clock timer that could rotate a tip nobody looked at.
- **Eleven card backgrounds**, chosen by what the tip is and when it arrived — a palette per
  hour, narrowed by the tip's kind, then the tip's own text picks within it, so a new tip still
  means a new-looking card. No pale card in the small hours; no Midnight in the morning. The
  whole ink set flips with the artwork, not with the phone's day/night theme.
- **Resizes** from a 2x2 square to a 4x4 block, with a layout built for each end of that range.
- **312 tips in nine pools** — six practical, scoped by time of day (`general` 50, `morning` 26,
  `afternoon` 26, `evening` 28, `sleep_late` 11, `sleep_early` 10), and three tone pools grouped
  by voice (`motivation` 59, `philosophy` 47, `wellbeing` 55).
- **A "Why this tip?" card in settings**: the research behind a practical tip, the text behind a
  quotation, or nothing at all for an original line.
- **No tip repeats within the last 100 shown**, and no tone voice runs three draws in a row.

Deliberately absent: accounts, streaks, gamification, history views, notifications, tracking.

## Architecture

Two Gradle modules, three features, the same three in both.

```
core/  pure Kotlin, JVM-only, zero Android imports
  tips/      Tip TipKind DayPart TipCatalog ToneProfile
             TipEngine AdvanceTipUseCase TipHistoryRepository
  settings/  AppSettings VarietyLevel SettingsRepository
  widget/    WidgetStyle TipRefreshSchedule WidgetRefreshRepository

app/   Android
  SapGlanceApp  AppContainer     composition root
  platform/                      the shared DataStore instance
  tips/      data/               DataStore impl of the :core interface
  settings/  data/  presentation/
  widget/    data/  presentation/  TipWidget RefreshTipAction
             framework/            TipWidgetReceiver BootReceiver
                                   WidgetRefreshWorker WidgetScheduler
```

`:core` declares the interfaces and `:app` implements them, so `:core` needs no Android SDK at
all and stays unit-testable on a plain JVM. It is flat per feature on purpose: a
`model`/`usecase`/`port` split was tried and reverted for turning 14 files into 8 folders, five
holding one file each (`git log` has the measurement).

`:app` splits by layer because those are real differences in kind. `framework/` holds the four
classes Android instantiates **by name** — their fully-qualified names are in the manifest, and
`TipWidgetReceiver`'s is the `ComponentName` every placed widget is bound to, so renaming it
drops widgets off home screens.

## How a tip is chosen

Three narrowing weighted picks: a **tier** (practical vs. tone), a **group** within it, then a
**tip**. Every group is filtered against the anti-repeat window *before* any draw, and a group
with nothing fresh left is dropped and its share redistributed among the survivors — weighting
first and filtering second was a real bug. If that still leaves nothing, the tone run limit
gives way before anti-repeat does: the first is a preference about which voice comes next, the
second is the product promise.

`VarietyLevel` sets the tone tier's share (20/50/80%) and never switches a tier off. Which tone
suits which hour is editorial rather than a setting: `ToneProfile` leads with motivation in the
morning and zeroes it at night. The reasoning for each of these sits in the KDoc where it
applies, and the behaviour is pinned by tests rather than by intention.

## Building

Kotlin · Compose (Material 3) · Glance · WorkManager · DataStore · Gradle Kotlin DSL with a
version catalog (AGP 8.10.1) · `minSdk 26`, `compileSdk`/`targetSdk 36`. Requires JDK 17.

```bash
./gradlew build        # everything, including assembleRelease
./gradlew test         # unit tests — :core 89, :app 6 per variant
./gradlew ktlintCheck  # formatting
./gradlew lint         # Android lint
```

CI (`.github/workflows/ci.yml`) runs all of it on every push and PR.

## Roadmap

Completed work lives in the git history. What's open:

- [ ] **Jokes, as a group inside `wellbeing`** rather than a fourth voice — that pool is already
      the only one allowed to be silly, so no new `ToneProfile` share has to be invented and they
      inherit its night weighting. Source them from public-domain humour rather than writing
      them; wellbeing's own rules bind, so the joke is aimed at the situation, never the reader.
- [ ] **A plain-English pass over the practical pools.** A tip held to ~90 characters *and* to
      what its two citations support drifts into the register of the abstract it came from.
      Rewriting has to re-check each line against its sources, and it orphans stored history, so
      do it in one pass rather than continuously.
- [ ] **Make a cold tap feel faster.** ~1s is process start plus Glance session setup, not app
      code, and `warmUp()` already hides the catalog parse behind it. No cheap answer left.
- [ ] **Languages beyond `en`.** The UI half is nearly free; the content half is the project —
      312 tips behind a `Locale`-blind classpath lookup, identified by their text everywhere it
      matters, citing English-language sources.
- [ ] **iOS port**, gated on hardware. The privacy promise doesn't translate literally (no iOS
      app can declaratively renounce network access) and WidgetKit has no background execution.

## License

MIT — see [LICENSE](LICENSE).
