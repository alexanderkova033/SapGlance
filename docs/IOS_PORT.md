# What an iOS port would actually take

Assessment written 2026-07-31, before any code. The roadmap carried this as one line ("gated on
hardware") for months, which was true and useless. This is the costing.

**Summary: the code is the easy part, and it is not what should decide this.** The domain layer
is ~90% portable for about a day's work. The widget is a full rewrite. Two of the things the
README leads with do not survive the platform, and that is a product decision rather than an
engineering one. On top of that sit three practical blockers, one of which is the same one
holding up the Play release.

## 1. What ports cleanly

`:core` is 1,125 lines of main source and it touches exactly three things that are not
multiplatform Kotlin:

| Where | What | Fix |
| --- | --- | --- |
| `TipEngine`, `AdvanceTipUseCase` | `java.time.LocalTime` | `kotlinx-datetime`, or `kotlin.time`. Two files, and only for "what hour is it". |
| `TipCatalog` | `getResourceAsStream` + `Charsets.UTF_8` | `expect`/`actual`, or generate the catalog into Kotlin source at build time and delete resource loading entirely. |
| tests | JUnit 5 + Truth | `kotlin.test` for anything moved to `commonTest`. 1,588 lines, so this is the bulk of the mechanical work. |

Everything else — `TipEngine`'s three-stage weighted selection, the anti-repeat window,
`ToneProfile`, `WidgetStyle`, `TipLanguage`, the repository *interfaces* — is already plain
Kotlin with `kotlinx-coroutines-core`, which is multiplatform. The dependency-inversion split
that exists for testability turns out to be exactly the seam a KMP port needs, which is luck
rather than foresight but is worth banking.

**Persistence is the one real design question.** `SettingsRepository` and
`TipHistoryRepository` are interfaces in `:core` with DataStore implementations in `:app`. iOS
gets a `UserDefaults` implementation of the same interfaces. No shared code changes.

Realistic effort: **1-2 days** to have the whole domain compiling and tested against an iOS
target, most of it spent rewriting tests rather than logic.

## 2. What has to be rebuilt from nothing

- **The widget.** SwiftUI + WidgetKit. No Compose or Glance code transfers.
- **The settings screen.** SwiftUI. It is one screen, so this is small.
- **All 19 background styles.** They are Android vector drawables (`<vector>` XML with
  gradients and paths). Nothing reads that format on iOS, so each one is redrawn as a SwiftUI
  `Canvas`/gradient or exported to an asset. This is the largest single chunk of work and it is
  art, not engineering — the constraint that matters is the one already written in the roadmap:
  each card has to survive the whole-card scrim and still read as artwork.

Realistic effort: **1-2 weeks**, dominated by the artwork.

## 3. The two promises that do not survive

This is the part worth deciding before anyone opens Xcode.

### The 90-minute screen-on rule breaks

SapGlance advances the tip after roughly **90 minutes of confirmed screen-on time** — the
README leads with this, and the point of it is explicit: "not a wall-clock timer rotating past
you while the phone is face down in a bag." On Android that is a WorkManager tick that checks
`PowerManager.isInteractive` and only counts the ticks where the screen was on.

**iOS has no equivalent and no near-equivalent.** A widget extension cannot observe screen
state, cannot run in the background, and is never told when it has been *looked at*. WidgetKit's
model is that you hand the system a timeline of future entries and it renders them on its own
schedule. The refresh budget itself is not the problem — 40-70 reloads a day against a rotation
that wants ~16 is comfortable. The problem is that none of those reloads can be conditioned on
whether the reader was actually there.

Three options, all of them a downgrade:

1. **Wall-clock timeline.** Precompute entries every 90 minutes. Simple, and it is precisely the
   behaviour the README defines the app against.
2. **Advance on interaction only.** iOS 17+ interactive widgets (a `Button` backed by an
   `AppIntent`) can advance the tip in-process without opening the app, so tap-to-refresh ports
   properly. But a widget nobody taps then never changes. Note also that reloading a timeline
   from inside `perform()` is reported as throttled on device, so even this is not free.
3. **Hybrid**: interaction, plus a slow wall-clock fallback. Best available, still not the
   promise.

### The offline guarantee stops being structural

On Android the claim is verifiable by anyone: there is no `INTERNET` permission in the manifest,
so the app *cannot* make a network call, and `CONTRIBUTING.md` makes that ground rule #1. iOS
has no such thing. Any app can open a socket; there is no entitlement to renounce and no
manifest line to omit. The closest available substitutes are the App Privacy nutrition label
(self-declared) and the user-facing App Privacy Report (after the fact).

So "100% offline, zero data collected" goes from *structurally true and checkable* to *our word
for it*. The store listing would have to be rewritten to stop implying otherwise, which is a
copy change that quietly gives away the strongest thing the product says about itself.

## 4. The practical blockers

- **A Mac.** Xcode does not run on anything else, and this project is developed on Windows. A
  Mac mini is around $599; a rented cloud Mac is $25-80/month. There is no free path that ends
  in an App Store binary.
- **Apple Developer Program, $99/year**, and — the important one — **enrollment requires the
  legal age of majority, exactly like Play**. The same guardian-owned-account route applies, so
  this is a *second* ask of the same person, with a second annual fee, before anything ships.
  Worth raising in the same conversation as the Play account rather than months later.
- **App Store review**, which is slower and more opinionated than Play's, and which the project
  has no experience of.

## 5. Recommendation

**Not before v1 is live on Android**, and the reason is not effort — it is that four of the
things this port would be built on are still unverified on a *phone that already exists*: the
Russian catalog, the language toggle, eight of the backgrounds, and whether the rotation feels
varied over weeks. Porting unverified behaviour to a second platform doubles the surface without
answering anything.

If it does happen, the order that wastes least:

1. Settle the two promises above **on paper**, and rewrite the listing copy to match, before any
   code. If the answer is "a wall-clock timer and a self-declared privacy claim", that is a
   different product and it is better to know that while it is still free to say no.
2. KMP-ify `:core` **as its own change on Android**, merged and shipped, with the Android app
   still passing its own tests against it. That is 1-2 days and it is useful regardless — it
   forces the last three JVM assumptions out of the domain.
3. Only then Xcode, and start with the artwork, because it is the long pole and the one that
   cannot be estimated from a spreadsheet.
