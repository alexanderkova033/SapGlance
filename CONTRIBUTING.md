# Contributing

Thanks for considering a contribution to SapGlance.

## Ground rules

These aren't up for debate on a per-PR basis — they're the whole point of the project:

1. **No `INTERNET` permission, ever.** If a change seems to need it, the change is wrong,
   not the rule.
2. **No analytics, crash-reporting, or advertising SDKs.**
3. **No new runtime dependencies** beyond AndroidX + the stack already in
   `gradle/libs.versions.toml` (Compose, Glance, WorkManager, DataStore) without a clear
   justification in the PR description.
4. **No streaks, gamification, or progress tracking.** v1 is intentionally passive.
5. Business logic (anything that isn't Android plumbing) belongs in `:core` and must have
   no `android.*` imports, so it stays unit-testable on the plain JVM.
6. All user-facing strings go in `strings.xml`, not hardcoded in Kotlin.

## Where a new file goes

Pick the feature first — `tips`, `settings` or `widget`, the same three in both modules — then:

- **In `:core`, that's the whole answer.** One folder per feature, flat. Don't add layer
  subfolders; that was tried and reverted, and the README's Architecture section says why.
- **In `:app`, pick a layer too**, because the kinds of file there are genuinely different:

| It is... | Goes in |
| --- | --- |
| the DataStore implementation of a `:core` interface | `<feature>/data/` |
| Compose or Glance UI | `<feature>/presentation/` |
| a class **Android** instantiates by name (receiver, worker, scheduler) | `<feature>/framework/` |

Business logic goes in `:core` regardless, per ground rule 5. The one hard structural rule is
the module boundary: `:core` declares the interfaces and `:app` implements them, so a `:core`
file never imports from `:app`. See the README's Architecture section for the full map and for
which class names are expensive to change (the OS binds four of them by name).

## Getting set up

```bash
git clone https://github.com/alexanderkova033/SapGlance.git
cd SapGlance
./gradlew build
```

Requires JDK 17. No Android device/emulator is required to build or run the unit tests.

## Before opening a PR

```bash
./gradlew ktlintCheck   # or: ./gradlew ktlintFormat to auto-fix
./gradlew lint
./gradlew test
```

CI runs the same three checks plus a full build on every push and PR.

## Commit style

This repo uses [Conventional Commits](https://www.conventionalcommits.org/)
(`feat:`, `fix:`, `chore:`, `docs:`, `test:`, ...). Keep the subject line under ~70
characters; use the body to explain *why*, not *what* (the diff already shows *what*).

## Adding a tip

Tip pools live in `core/src/main/resources/tips/*.txt`, one tip per line. **First decide which
kind of tip you're adding** (`TipKind`), because it determines both the file and the evidence
you owe:

| Kind | Pools | Citation required |
| --- | --- | --- |
| `PRACTICAL` | `general` / `morning` / `afternoon` / `evening` / `sleep_*` | Yes, at least `Tip.MIN_SOURCES` (2) independent sources |
| `MOTIVATION` | `motivation.txt` | No, and none may be implied |
| `PHILOSOPHY` | `philosophy.txt` | Only for a quotation: exactly the text it's quoted from |
| `WELLBEING` | `wellbeing.txt` | No, and none may be implied |

The rule of thumb: **if it asserts a fact about your body or mind, it's `PRACTICAL` and needs
sources. If it's encouragement, reflection, or a quotation, it's a tone tip and needs an
attribution at most.** Don't smuggle an empirical claim into a tone pool to dodge the citation
requirement — that's precisely what the split exists to prevent, and `TipCatalogTest` checks
each rule separately.

**Practical pools** use a two-file layout: each has a companion `<name>_sources.txt`, one line
per tip in the same order, holding one or more `Label<TAB>URL` pairs tab-separated end to end.
`TipCatalog.loadDefault()` zips the two line-for-line and fails loudly (`require`) if the counts
don't match or a line has an odd field count.

**Tone pools** put any attribution inline on the tip's own line (`Text<TAB>Label<TAB>URL`), since
most of their lines have no source at all and a mostly-blank companion file would silently drift
out of alignment (blank lines get stripped on load).

Either way the citation isn't optional bookkeeping: it powers the settings screen's "Why this
tip?" card. See [TIP_SOURCES.md](TIP_SOURCES.md) for the underlying research and the
source-quality rules (primary study over press release; no Wikipedia, ResearchGate, or
aggregator pages; consumer health sites only alongside a peer-reviewed primary).

Keep new tips:

- Non-numeric / non-statistical (no fabricated stats) — a real, well-established figure
  (e.g. a caffeine half-life, a recommended bedroom temperature range) is fine; an
  invented-sounding one is not.
- Phrased as a gentle suggestion ("can help", "is linked to"), never a guaranteed outcome.
- Short enough to read at a glance in a small widget: **under ~90 characters**. The whole
  catalog was shortened to that ceiling after long tips were seen clipping on a real device
  while still obeying the previous ~115 guidance. 90 is a calibration rather than a measured
  budget, so leave headroom rather than spending it, and prefer the shorter phrasing when two
  say the same thing. Nothing enforces it: `TipCatalogTest` has no length assertion, because
  the number to encode should be measured on-device at the smallest supported widget size
  first.
- **Interesting, not just true** (practical pools). Accuracy is the floor, not the bar: a widget
  reappears several times a day for months, so a line that informs once and then reads as
  wallpaper is a real cost. Every practical tip should **bust a myth**, **name a mechanism**,
  **carry a real number**, or **invert an assumption**. Keep the topic and change the register:
  posture, hydration, breaks and morning light are clichés precisely because they are true, so
  don't drop them, change what the line says about them. The full rule, with worked examples,
  is the header comment at the top of `tips/general.txt`; the other five practical files point
  at it and add their own warning about the trap specific to that pool (a cluster to watch, or
  in the two night files, what not to restate from `evening.txt`). This is enforced by review, not by a test, because
  a lint for "boring" would be gameable nonsense.
- Free of em dashes. `TipCatalogTest` fails the build on one; use a comma, a period, or "and".
- Distinct from what's already there. `TipCatalogTest` only catches byte-identical duplicates,
  so check by hand that you aren't restating an existing tip in different words — and if the
  point is worth making twice, make the two tips differ in *register* (the action vs. the
  evidence behind it), not just in phrasing.
- For a quotation: traceable to a specific chapter or letter in a public-domain edition, and
  cited to that edition. Popular philosophy quotes are misattributed constantly; if you can't
  pin it to a passage, don't use it.

Each tone pool has a **writing rule** recorded as a header comment in its own file. Read it
before adding to that pool — they exist because the first draft of each got these wrong:

- **`wellbeing.txt`**: never assume the reader is having a bad day. The widget appears at
  random, so a line presupposing struggle ("it's fine if today was mostly getting through it")
  lands as a diagnosis nobody asked for, and a corrective line ("nobody is watching you as
  closely as you think") tells them they were wrong about something. Offer something small,
  concrete, and equally welcome on a good day. Hands, windows, sound, temperature, a song.
- **`motivation.txt`**: this pool pushes. Short sentences, verbs up front. Permission-giving
  lines ("rest is part of the work") are warm but not motivating, and belong in `wellbeing.txt`
  where they won't dilute this pool's job.
- **`philosophy.txt`**: quote actual philosophers, in the public-domain edition's actual
  wording. Original reflections are allowed but stay a minority, which `TipCatalogTest`
  enforces. Verify before quoting: "we are what we repeatedly do" is Will Durant summarising
  Aristotle rather than Aristotle, and "he who has a why to live can bear almost any how" is
  Viktor Frankl's paraphrase, not Nietzsche's sentence.

Each tone pool also has a **second header rule about variety**, added after all three drifted
the same way: a pool fills up with whichever line is easiest to write in that voice, and ends
up feeling far smaller than its tip count suggests. Nothing automated catches this, so it is
on you to check which angle a new line is before adding it:

- **`motivation.txt`**: "just start" is the easy line, and it was once ten of eighteen tips.
  Starting is capped at a handful; the rest of the pool covers continuing through the boring
  middle, coming back after a lapse, finishing, reps over talent, comparison, and saying the
  thing out loud.
- **`wellbeing.txt`**: the easy line is another quiet indoor noticing prompt. Keep the spread
  across noticing, the body, outside, play, other people, and small acts of making things
  nicer, and prefer whichever group is thinnest.
- **`philosophy.txt`**: the easy line is another Stoic one — the pool was once entirely Stoic,
  which is a school, not a subject. It now also carries Confucian, Buddhist, Taoist,
  transcendentalist and essayist voices. Before adding another Roman, check whether some
  tradition in the file is down to a single line.

The file groups its tips under `#` comment headings by angle or tradition. Those headings are
stripped at load time like any other comment and have no effect on selection; they exist so
the thin group is visible at a glance when you're deciding what to add.
