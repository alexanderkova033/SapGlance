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
 */
data class TipCatalog(
    val general: List<Tip>,
    val morning: List<Tip>,
    val afternoon: List<Tip>,
    val evening: List<Tip>,
    val sleepLate: Tip,
    val sleepEarlyHours: Tip,
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
     * freshly concatenated list of all 282 tips, which is fine for one Settings lookup and wrong
     * for something a widget tap waits on.
     */
    private val kindsByText: Map<String, TipKind> by lazy {
        val all = general + morning + afternoon + evening + tonePools + listOf(sleepLate, sleepEarlyHours)
        all.associate { it.text to it.kind }
    }

    /**
     * Null for a text this catalog doesn't know — a tip that has since been reworded or dropped
     * can still be sitting in a user's persisted history, and that must degrade to "no opinion"
     * rather than throwing.
     */
    fun kindOf(text: String): TipKind? = kindsByText[text]

    companion object {
        fun loadDefault(): TipCatalog =
            TipCatalog(
                general = loadPool("general.txt"),
                morning = loadPool("morning.txt"),
                afternoon = loadPool("afternoon.txt"),
                evening = loadPool("evening.txt"),
                sleepLate = loadSingle("sleep_late.txt"),
                sleepEarlyHours = loadSingle("sleep_early.txt"),
                motivation = loadTonePool("motivation.txt", TipKind.MOTIVATION),
                philosophy = loadTonePool("philosophy.txt", TipKind.PHILOSOPHY),
                wellbeing = loadTonePool("wellbeing.txt", TipKind.WELLBEING),
            )

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
            kind: TipKind,
        ): List<Tip> =
            resourceLines(fileName).map { line ->
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

        private fun loadPool(fileName: String): List<Tip> {
            val texts =
                resourceLines(fileName).also {
                    require(it.isNotEmpty()) { "Tip pool 'tips/$fileName' must not be empty" }
                }
            return zipWithSources(fileName, texts)
        }

        private fun loadSingle(fileName: String): Tip {
            val texts = resourceLines(fileName)
            require(texts.size == 1) {
                "Tip resource 'tips/$fileName' must contain exactly one message, found ${texts.size}"
            }
            return zipWithSources(fileName, texts).first()
        }

        private fun zipWithSources(
            fileName: String,
            texts: List<String>,
        ): List<Tip> {
            val sourceFileName = sourceFileNameFor(fileName)
            val sources = resourceLines(sourceFileName)
            require(sources.size == texts.size) {
                "'tips/$fileName' has ${texts.size} tips but 'tips/$sourceFileName' has " +
                    "${sources.size} source entries — they must match line-for-line"
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

        private fun resourceLines(fileName: String): List<String> {
            val stream =
                requireNotNull(TipCatalog::class.java.getResourceAsStream("/tips/$fileName")) {
                    "Missing bundled tip resource: tips/$fileName"
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
