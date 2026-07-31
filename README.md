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
- **316 tips in nine pools** — six practical, scoped by time of day (`general` 50, `morning` 26,
  `afternoon` 26, `evening` 28, `sleep_late` 11, `sleep_early` 10), and three tone pools grouped
  by voice (`motivation` 59, `philosophy` 47, `wellbeing` 59).
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
./gradlew test         # unit tests — :core 90, :app 6 per variant
./gradlew ktlintCheck  # formatting
./gradlew lint         # Android lint
```

CI (`.github/workflows/ci.yml`) runs all of it on every push and PR.

## Roadmap

### Getting to release, in order

The code is not what is blocking. The schedule is: a personal Play account created after
Nov 2023 must run a closed test with **12 testers opted in for 14 consecutive days** before
production, and that clock only starts at the first upload. So the ordering below is by what
unblocks what, and content work deliberately waits for the test window.

1. **Back up the signing key.** `keystore/healthwidget-release.jks` *and*
   `keystore/keystore.properties` — the key is useless without the passwords. Two copies, at
   least one off this machine. It is the only unrecoverable item in the project: lose it and the
   listing can never be updated again, only replaced under a new `applicationId`. Never in the
   repo (it is gitignored, and has never been committed — checked).
2. **Start the guardian conversation.** The account holder must be 18+, so the account is theirs
   with the developer added as a user. $25 once, an identity check, and their name and address
   appear publicly on the listing. Longest lead time of anything here, so it starts first even
   though it finishes last.
3. **Merge to `main` and push.** CI and the Pages workflow only run there.
4. **Turn on Pages**: Settings → Pages → Source → "GitHub Actions". Publishes `PRIVACY.md`,
   which Play requires at a public URL. Nothing happens until step 3.
5. **Screenshots**, on a *clean* home screen — a listing is public, and the obvious shot also
   publishes your wallpaper, your apps and your location. See `store-assets/play-listing.md` for
   the framing and the 2:1 rule Play enforces.
6. **Export the feature graphic** from `store-assets/play-feature-graphic-1024x500.svg` to PNG
   at exactly 1024x500.
7. **Upload, recruit 12 testers, wait 14 days.**
8. **During the wait**, run the plain-English pass. It needs a fortnight of real use first
   anyway, and a version boundary is the right place to land something that resets tip history.

### Open work

Completed work lives in the git history. What's open:

- [ ] **More card backgrounds.** Eleven is thin now that selection narrows by hour and kind:
      the night palette is only four styles, so two consecutive night tips share a background
      one time in four, and all three daylight palettes lean on the same four pale cards. Most
      wanted, in order: **more deep, quiet artwork for the night palette**, then **more pale
      styles**, since four are currently doing the work of every daylight hour.
      Adding one is three edits and each is enforced rather than remembered: a `WidgetStyle`
      entry (the constructor makes you declare `isLight`), a drawable in `:app`'s exhaustive
      `when` (compile error if missing), and membership of at least one palette (a test fails if
      a style is unreachable, so new artwork cannot ship dead). Ink is derived from `isLight`,
      so there is no second colour choice to get wrong. The one thing no check covers: the art
      has to survive the whole-card scrim it will be drawn under, 0.42 black on a dark style and
      0.30 white on a pale one, and still read as artwork rather than as texture.
- [ ] **Grow the jokes group in `wellbeing`.** Started: the group exists with four sourced lines,
      and `WELLBEING` may now carry a single attribution the way a quoted `PHILOSOPHY` line does,
      since sourcing them and claiming them as ours are not compatible. The constraint that makes
      this slow was not costed in the original plan: public-domain humour short enough for a
      widget is almost all Victorian epigram, which is *witty* rather than *warm*, and wellbeing's
      voice is warm. Filling the group with Wilde and Bierce would quietly turn the pool acid, and
      no test catches that. Grow it on one question — does the line still sound like it belongs
      beside "check on the plant"? Jerome K. Jerome passes easily; Bierce is capped at one.
- [ ] **A plain-English pass over every pool**, not just the practical ones. The reading half is
      done: [docs/TIGHTENING_AUDIT.md](docs/TIGHTENING_AUDIT.md) has the patterns, worked
      rewrites, and the split between safe compression and rewrites that touch the claim and so
      need their sources re-read. Two things it settled: 39 tips are quotations and can never be
      reworded, and the slack is overwhelmingly on the practical side (mean 75-83 characters
      against the tone pools' 61-63, and four times the hedging) because fidelity to two
      citations inside ~90 characters is what produces that register. Do it in one pass, since
      rewording orphans stored history and restyles every card.
- [ ] **Make a cold tap feel faster.** ~1s is process start plus Glance session setup, not app
      code, and `warmUp()` already hides the catalog parse behind it. No cheap answer left.
- [ ] **Languages beyond `en`.** The UI half is nearly free; the content half is the project —
      316 tips behind a `Locale`-blind classpath lookup, identified by their text everywhere it
      matters, citing English-language sources.
- [ ] **iOS port**, gated on hardware. The privacy promise doesn't translate literally (no iOS
      app can declaratively renounce network access) and WidgetKit has no background execution.

## License

MIT — see [LICENSE](LICENSE).
