# Handoff prompt: rework tip selection now that the tone pools exist

> **Status: carried out.** This brief has been acted on — see README "Notable design decisions"
> for what was built and what was rejected, and STATUS.md for the pass that did it. It is kept
> as the record of what was asked for, not as outstanding work. Everything below the line
> describes the model as it stood *before* that pass, so read it as history: the pool counts
> are stale, `TipEngine` no longer has a `selectGroup` coin flip, and the four numbered problems
> are the ones that were solved. Two things it asked about were deliberately left open — growing
> the practical sleep-hours pools (content work at the citation bar) and confirming the
> tap-to-refresh lag on a real device.

Paste everything below the line into a fresh chat, in the repo root. It is written to be
self-contained: it states the current behaviour, the known problems, the constraints, and what
"done" looks like, without assuming the other chat has any memory of this one.

Before pasting, re-read `core/src/main/kotlin/com/sapglance/core/tips/TipEngine.kt` yourself
and correct anything below that has drifted. A stale prompt is worse than no prompt.

---

## Context

This is SapGlance, a privacy-first Android wellness app (Kotlin, Glance widget, no network,
no accounts). A home-screen widget shows one rotating tip. All tip-selection logic lives in the
pure-JVM `:core` module and is fully unit-tested; there is no Android dependency in it.

Read these first, in order:

- `core/src/main/kotlin/com/sapglance/core/tips/TipEngine.kt` — all selection logic
- `core/src/main/kotlin/com/sapglance/core/tips/TipCatalog.kt` — pools and how they load
- `core/src/main/kotlin/com/sapglance/core/tips/TipKind.kt` — what kinds of tip exist
- `core/src/main/kotlin/com/sapglance/core/settings/VarietyLevel.kt` — the user-facing setting
- `core/src/test/kotlin/com/sapglance/core/tips/TipEngineTest.kt` — existing guarantees
- README.md, "Notable design decisions"

## What exists right now

**Seven pools.** Four practical, day-part scoped (`general` 50 tips, `morning` 26, `afternoon`
26, `evening` 28) plus two single fixed sleep-hours messages. Three *tone* pools grouped by
voice rather than time: `motivation` (53), `philosophy` (42), `wellbeing` (55).
`TipCatalog.tonePools` concatenates the three, so the tone blob is 150 tips against a
day-part practical pool of 76-78.

**Every tip has a `TipKind`** (`PRACTICAL` / `MOTIVATION` / `PHILOSOPHY` / `WELLBEING`), which
governs how it may be presented: `PRACTICAL` tips must cite at least two independent research
sources, tone tips cite nothing (or, for a quotation, the public-domain text it came from).
`TipCatalogTest` enforces this. **Do not weaken those tests to make a selection change easier.**

**Selection today** (`TipEngine.messageFor` → `pick` → `selectGroup`):

1. Day part picks the practical pool: `general + <daypart>`.
2. Anti-repeat filters *both* the practical pool and the combined tone pool against the last 30
   shown tips (`TipHistoryRepository.MAX_RECENT_TIPS`).
3. A weighted coin flip picks *which group* to draw from. `VarietyLevel` sets the tone group's
   odds: `PRACTICAL` 20%, `BALANCED` 50%, `PLAYFUL` 80%.
4. Uniform random pick within the winning group.
5. If neither group has anything unseen, it falls back to the weighted full pools and repeats.

Two properties here are deliberate and load-bearing. Keep both:

- **Variety is a lean, never a filter.** Even `PRACTICAL` lets a tone tip through sometimes;
  even `PLAYFUL` still shows practical tips. No level ever means "only this."
- **Anti-repeat is applied to both pools *before* the group coin flip, not after.** Doing it
  the other way round was a real fixed bug: the flip could land on a group whose only unseen
  tips had just been shown, forcing a repeat while the other group had fresh tips sitting
  unused. `TipEngineTest` has a regression case for this ("prefers an unseen tip from the other
  group over repeating within the weighted group").

## The problems to solve

1. **The three tone pools are one undifferentiated blob.** A `PLAYFUL` draw is equally likely to
   be a motivational push ("Stop researching it. Go and do it badly."), a Stoic quotation, or a
   quiet wellbeing invitation ("Put both hands around something warm and stay there for a
   minute."). Those land very differently by hour: the motivation line at 23:40 is the right
   sentence at completely the wrong time, and the wellbeing line is a strange thing to be handed
   at 9am on a Monday. Nothing currently takes time of day into account for tone tips at all.
   Note the three pools have distinct editorial voices on purpose — each file carries its
   writing rule as a header comment, and `philosophy.txt` is majority real quotations with a
   test enforcing that. Treat the three as genuinely different things, not three names for
   "not practical."
2. **Uniform random within a pool feels repetitive in use.** With a 30-tip anti-repeat window
   against a practical pool of 63-64, the tail end of the window is heavily constrained and the
   same tips can cluster. The user has reported the rotation feeling less varied than the pool
   sizes suggest it should. Consider recency weighting, or a shuffled-bag / deck-of-cards
   approach (deal the pool in a random permutation, reshuffle when exhausted) rather than
   independent draws. Note both sides of the catalog grew after this brief was written: the
   tone pools from 54 tips to 150, and the practical pools from 109 to 130. That relieves the
   pressure but does nothing about the underlying independent-draw behaviour, which is the
   actual complaint.
3. **`VarietyLevel` has three labels but four kinds to distribute across.** Decide whether the
   user should get finer control (per-group weights) or whether that is over-configuration for
   an app whose whole philosophy is minimal settings. **Recommendation: do not add more
   settings** without a strong reason; make the defaults smarter instead. If you disagree,
   argue it explicitly rather than quietly adding a slider.
4. **Tone tips ignore the sleep-hours day parts.** 23:00-05:59 shows one fixed practical message
   (unless the user explicitly taps for a new tip, which draws from `general`). A wellbeing or
   philosophy line is arguably a much better fit for 2am than a wellness tip is. There is an
   open roadmap item to turn the sleep messages into small pools; this is the natural moment.

## Constraints, non-negotiable

- `:core` stays pure Kotlin/JVM. No Android imports, no new third-party dependencies.
- `TipEngine` stays deterministic under an injected `kotlin.random.Random`, so tests can assert
  behaviour without statistics. Any new state must be passed in, not read from a clock or a
  singleton. If selection needs to remember more than "the last 30 texts", that state goes
  through a repository interface in `:core` with the DataStore implementation in `:app`, per the
  existing `TipHistoryRepository` pattern.
- Anti-repeat (never repeat within the last 30 shown) must still hold. It is a stated product
  guarantee (FR5) and every call site depends on `AdvanceTipUseCase` for it.
- Do not weaken `TipCatalogTest`'s per-kind citation rules.
- `./gradlew ktlintCheck test lint build` must be green. The project has no local JDK/Android
  SDK by default; see STATUS.md "Local environment notes" for the scratch-JDK setup.

## Also worth knowing

- There is an **unconfirmed performance suspicion** already recorded in ROADMAP.md:
  tapping the widget for a new tip reportedly feels laggy, and the prime suspect is
  `TipCatalog.loadDefault()` re-parsing all bundled resource files on a cold process start, not
  the selection algorithm itself. **Confirm with real timing/logcat evidence before treating
  either as the cause.** Do not assume the algorithm is the bottleneck just because you are
  working on the algorithm.
- Adding the tone pools changed default behaviour: at the default `VarietyLevel.PRACTICAL`,
  roughly 20% of tips shown are now tone tips where previously it was 0% (the pools were empty).
  Whether 20% is the right default at `PRACTICAL` is an open question worth revisiting with
  fresh eyes.

## Definition of done

- A written rationale for the selection model you chose, in README "Notable design decisions",
  in the same register as the entries already there: what you chose, what you rejected, why.
- Unit tests in `TipEngineTest` covering the new behaviour, including a regression test for any
  bug you actually find and fix.
- The existing guarantees above still enforced by tests, not just by intention.
- `./gradlew ktlintCheck test lint build` green.
- STATUS.md updated with what changed and what is still unverified on a physical device.

Start by reading the files listed at the top and telling me what you think the selection model
should be, with trade-offs, **before** writing any code.
