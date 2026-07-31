package com.sapglance.core.tips

/**
 * Bundled tip content, grouped by [DayPart]. Loaded from plain-text resources (one tip per
 * line, `#` for comments) rather than JSON: the catalog is a handful of static string lists,
 * so a hand-rolled line reader avoids pulling a JSON parsing dependency into a module whose
 * whole point is to stay dependency-free and trivially JVM-testable.
 *
 * Each `tips/<name>.txt` has a companion `tips/<name>_sources.txt` of the same length, zipped
 * in line-for-line. A source line is one or more `Label<TAB>URL` pairs, tab-separated end to
 * end (so a line has an even number of fields), which keeps the one-line-per-tip correspondence
 * intact now that a tip carries several citations rather than one — see `TIP_SOURCES.md` at the
 * repo root for the research those citations trace back to.
 *
 * ## Languages
 *
 * English lives at the root of `tips/` and every other language in a `tips/<language>/` folder
 * beneath it, under the same file names. The asymmetry is the point rather than an accident of
 * growth: English is the text the citations were checked against, and the other languages are
 * translations *of it*, so they sit under it and are read against the same evidence.
 *
 * **`_sources.txt` is not translated and there is only ever one copy of it**, shared by every
 * language of the pool it belongs to. Two consequences worth knowing before adding a language.
 * The good one: a translation cannot silently drift out of alignment with the evidence, because
 * the same file has to zip line-for-line against both, so dropping or reordering a line in a
 * translation fails at load. The awkward one: a Russian reader who opens "why this tip?" gets
 * English citations. That is the honest outcome — the study is in English and pretending
 * otherwise by translating the journal's name would make the citation harder to check, not
 * easier — but it is a real rough edge rather than a design triumph.
 */
data class TipCatalog(
    val general: List<Tip>,
    val morning: List<Tip>,
    val afternoon: List<Tip>,
    val evening: List<Tip>,
    // The two night pools. They were one fixed message each for most of this project's life,
    // which is why so much around them reads as a special case; they are ordinary day-part pools
    // now, and the only thing still special about them is that `general` is not mixed in (see
    // TipEngine.practicalGroups).
    val sleepLate: List<Tip>,
    val sleepEarlyHours: List<Tip>,
    // The three "tone" pools the "more variety" setting leans towards (see TipEngine.pick).
    // Unlike the day-part pools above these are grouped by voice rather than by time of day,
    // and none of them are TipKind.PRACTICAL, so they're exempt from the citation requirement
    // (see TipKind). Still defaulted to empty so test constructions can opt in to just the
    // pools a given case cares about.
    val motivation: List<Tip> = emptyList(),
    val philosophy: List<Tip> = emptyList(),
    val wellbeing: List<Tip> = emptyList(),
) {
    /** Every tone tip, in one list — what [TipEngine] weighs against the practical pools. */
    val tonePools: List<Tip> get() = motivation + philosophy + wellbeing

    /**
     * Text to kind, for the history — which stores plain text and nothing else — to be read back
     * as kinds. [TipEngine] needs this on the selection path to enforce its run limit, so it is
     * built once per catalog and cached rather than re-derived per lookup: `findByText` walks a
     * freshly concatenated list of every tip in the catalog, which is fine for one Settings lookup and wrong
     * for something a widget tap waits on.
     */
    private val kindsByText: Map<String, TipKind> by lazy {
        val all = general + morning + afternoon + evening + sleepLate + sleepEarlyHours + tonePools
        all.associate { it.text to it.kind }
    }

    /**
     * Null for a text this catalog doesn't know — a tip that has since been reworded or dropped
     * can still be sitting in a user's persisted history, and that must degrade to "no opinion"
     * rather than throwing.
     */
    fun kindOf(text: String): TipKind? = kindsByText[text]

    companion object {
        /** The language the citations were checked against, and the fallback for anything else. */
        const val DEFAULT_LANGUAGE = "en"

        /**
         * ISO 639-1 codes with a bundled catalog. Anything else falls back to [DEFAULT_LANGUAGE]
         * rather than throwing: a device set to a language this app has never heard of must show
         * English tips, not crash on a missing resource.
         */
        val SUPPORTED_LANGUAGES = setOf(DEFAULT_LANGUAGE, "ru")

        /**
         * [language] is an ISO 639-1 code, not a locale tag: `ru-RU` and `ru-KZ` read the same
         * catalog, because what varies between them is date and number formatting rather than
         * anything this app writes. Callers pass `Locale.getDefault().language`, which already
         * gives the bare code.
         */
        fun loadDefault(language: String = DEFAULT_LANGUAGE): TipCatalog {
            val resolved = if (language in SUPPORTED_LANGUAGES) language else DEFAULT_LANGUAGE
            return TipCatalog(
                general = loadPool("general.txt", resolved),
                morning = loadPool("morning.txt", resolved),
                afternoon = loadPool("afternoon.txt", resolved),
                evening = loadPool("evening.txt", resolved),
                sleepLate = loadPool("sleep_late.txt", resolved),
                sleepEarlyHours = loadPool("sleep_early.txt", resolved),
                motivation = loadTonePool("motivation.txt", resolved, TipKind.MOTIVATION),
                philosophy = loadTonePool("philosophy.txt", resolved, TipKind.PHILOSOPHY),
                wellbeing = loadTonePool("wellbeing.txt", resolved, TipKind.WELLBEING),
            )
        }

        /**
         * Tip text is per-language, citations are not — see the class doc. English sits at the
         * root of `tips/` rather than in an `en/` folder of its own, which keeps the companion
         * `_sources.txt` beside the text it was checked against.
         */
        private fun tipPath(
            fileName: String,
            language: String,
        ) = if (language == DEFAULT_LANGUAGE) "/tips/$fileName" else "/tips/$language/$fileName"

        /**
         * Tone pools keep their (usually absent) attribution inline on the tip's own line,
         * `Text[<TAB>Label<TAB>URL]...`, rather than in a companion `_sources.txt`. Most lines
         * in these pools have no source at all, so a parallel file would be almost entirely
         * blank, and blank lines are exactly what [resourceLines] strips — the two files would
         * silently stop lining up. The practical pools have the opposite shape (every tip has
         * 2+ sources), which is why they keep the two-file layout.
         */
        private fun loadTonePool(
            fileName: String,
            language: String,
            kind: TipKind,
        ): List<Tip> =
            resourceLines(tipPath(fileName, language)).map { line ->
                val fields = line.split("\t")
                val sources = fields.drop(1)
                require(sources.size % 2 == 0) {
                    "Malformed line in 'tips/$fileName': \"$line\" (attribution must be whole " +
                        "\"Label<TAB>URL\" pairs)"
                }
                Tip(
                    text = fields.first(),
                    kind = kind,
                    sources =
                        sources.chunked(2) { (label, url) ->
                            require(label.isNotBlank() && url.isNotBlank()) {
                                "Blank label or URL in 'tips/$fileName': \"$line\""
                            }
                            TipSource(label = label, url = url)
                        },
                )
            }

        private fun loadPool(
            fileName: String,
            language: String,
        ): List<Tip> {
            val texts =
                resourceLines(tipPath(fileName, language)).also {
                    require(it.isNotEmpty()) { "Tip pool 'tips/$fileName' must not be empty" }
                }
            return zipWithSources(fileName, language, texts)
        }

        /**
         * The citations are shared across languages, so this is also the check that a translation
         * has not lost, gained or reordered a line: the same `_sources.txt` has to zip against
         * every language of the pool, and the message names the language so a failure points at
         * the file that actually drifted.
         */
        private fun zipWithSources(
            fileName: String,
            language: String,
            texts: List<String>,
        ): List<Tip> {
            val sourceFileName = sourceFileNameFor(fileName)
            val sources = resourceLines("/tips/$sourceFileName")
            require(sources.size == texts.size) {
                "'${tipPath(fileName, language)}' has ${texts.size} tips but " +
                    "'tips/$sourceFileName' has ${sources.size} source entries — they must " +
                    "match line-for-line, and that file is shared by every language"
            }
            return texts.zip(sources) { text, sourceLine -> text.toTip(sourceLine, sourceFileName) }
        }

        private fun sourceFileNameFor(fileName: String) = fileName.removeSuffix(".txt") + "_sources.txt"

        private fun String.toTip(
            sourceLine: String,
            sourceFileName: String,
        ): Tip {
            val fields = sourceLine.split("\t")
            require(fields.size >= Tip.MIN_SOURCES * 2 && fields.size % 2 == 0) {
                "Malformed line in 'tips/$sourceFileName': \"$sourceLine\" (expected at least " +
                    "${Tip.MIN_SOURCES} tab-separated \"Label<TAB>URL\" pairs)"
            }
            val sources =
                fields.chunked(2) { (label, url) ->
                    require(label.isNotBlank() && url.isNotBlank()) {
                        "Blank label or URL in 'tips/$sourceFileName': \"$sourceLine\""
                    }
                    TipSource(label = label, url = url)
                }
            return Tip(text = this, sources = sources)
        }

        private fun resourceLines(path: String): List<String> {
            val stream =
                requireNotNull(TipCatalog::class.java.getResourceAsStream(path)) {
                    "Missing bundled tip resource: $path"
                }
            return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
            }
        }
    }
}
