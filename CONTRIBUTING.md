# Contributing

## Ground rules

Not up for debate on a per-PR basis — they're the point of the project:

1. **No `INTERNET` permission, ever.** If a change seems to need it, the change is wrong.
2. **No analytics, crash-reporting or advertising SDKs.**
3. **No new runtime dependencies** beyond what's already in `gradle/libs.versions.toml`, without
   a justification in the PR description.
4. **No streaks, gamification or progress tracking.** v1 is intentionally passive.
5. **Business logic belongs in `:core`**, with no `android.*` imports, so it stays unit-testable
   on a plain JVM.
6. **User-facing strings go in `strings.xml`**, not hardcoded in Kotlin.

## Building and testing

Kotlin · Compose (Material 3) · Glance · WorkManager · DataStore · Gradle Kotlin DSL with a
version catalog (AGP 8.10.1) · `minSdk 26`, `compileSdk`/`targetSdk 36`.

```bash
git clone https://github.com/alexanderkova033/SapGlance.git
cd SapGlance

./gradlew build        # everything, including assembleRelease
./gradlew test         # unit tests — :core 102, :app 6 per variant
./gradlew ktlintCheck  # formatting (ktlintFormat auto-fixes)
./gradlew lint         # Android lint
```

Requires JDK 17. No device or emulator is needed to build or run the unit tests.

Before opening a PR, run `./gradlew ktlintCheck lint test`. CI
(`.github/workflows/ci.yml`) runs the same three plus a full build on every push and PR.
Commits follow [Conventional Commits](https://www.conventionalcommits.org/); keep the subject
under ~70 characters and use the body to explain *why*, not *what*.

## Where a new file goes

Pick the feature first — `tips`, `settings` or `widget`, the same three in both modules. In
`:core` that's the whole answer: one flat folder per feature, no layer subfolders. In `:app`,
pick a layer too:

| It is... | Goes in |
| --- | --- |
| the DataStore implementation of a `:core` interface | `<feature>/data/` |
| Compose or Glance UI | `<feature>/presentation/` |
| a class Android instantiates by name (receiver, worker, scheduler) | `<feature>/framework/` |

## Adding a tip

Pools live in `core/src/main/resources/tips/*.txt`, one tip per line. The kind decides both the
file and the evidence you owe:

| Kind | File | Citation |
| --- | --- | --- |
| `PRACTICAL` | `general` / `morning` / `afternoon` / `evening` / `sleep_*` | 2+ independent sources |
| `MOTIVATION` | `motivation.txt` | none, and none may be implied |
| `PHILOSOPHY` | `philosophy.txt` | only for a quotation: the text it's quoted from |
| `WELLBEING` | `wellbeing.txt` | none, and none may be implied |

If it asserts a fact about your body or mind, it's `PRACTICAL`. Don't smuggle an empirical claim
into a tone pool to dodge the citation requirement — that is exactly what the split exists to
prevent, and `TipCatalogTest` checks each rule separately.

Practical pools keep citations in a companion `<name>_sources.txt`: one line per tip in the same
order, `Label<TAB>URL` pairs tab-separated, and the loader fails loudly if the two files drift
apart. Tone pools put any attribution inline on the tip's own line. See
[TIP_SOURCES.md](TIP_SOURCES.md) for the research behind each claim and the source-quality rules
(primary study over press release; no Wikipedia or aggregator pages).

**A new tip is a new line in every language.** English is the source of truth and lives at the
root of `tips/`; each translation is the same file name under `tips/<language>/`. The citations
are *not* translated and there is one copy of them, zipped by position against every language,
so a tip added to `general.txt` without a matching line in `ru/general.txt` fails at load rather
than mis-citing. That is deliberate: it is cheaper to be stopped by the build than to ship a
Russian tip carrying the previous tip's evidence.

## Adding a language

Three steps, and the third is the work.

1. `TipCatalog.SUPPORTED_LANGUAGES` and `app/src/main/res/xml/locales_config.xml`. Nothing
   enforces that these agree, because one is Kotlin in a module with no Android on its
   classpath and the other is an Android resource. Listing a language in only the second offers
   the reader a translation that silently falls back to English.
2. `app/src/main/res/values-<language>/strings.xml` for the settings screen and the widget's
   description. 27 strings; note that plurals may need quantities English does not have.
3. `core/src/main/resources/tips/<language>/`, nine files, line-for-line with the English. Read
   `tips/ru/general.txt`'s header first — it records the three rules the Russian ran under (no
   em dash, no longer than the English, the hedge is not decoration), and
   `tips/ru/philosophy.txt`'s header records the one genuinely awkward decision in the whole
   design: what a translated *quotation* is allowed to claim when its citation stays English.

`TipCatalogTest` is parameterized over every supported language, so a translation is held to
every invariant English is, plus three of its own: same shape, identical citations, and actually
translated.

Every tip: under ~90 characters, phrased as a suggestion ("can help", "is linked to") rather
than a promise, no em dashes (`TipCatalogTest` fails the build on one), no invented statistics,
and genuinely distinct from what's there — the test only catches byte-identical duplicates, so
restating an existing tip in different words is on you to spot.

**Read the target file's header comment before adding to it.** Every pool records its own
writing rules there: what a practical tip has to do beyond being true, what each tone voice is
for, and which angle that pool over-fills if nobody is watching. They exist because each pool
got it wrong first, and they're the part no test can enforce.
