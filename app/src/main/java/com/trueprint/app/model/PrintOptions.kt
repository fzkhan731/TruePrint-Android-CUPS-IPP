package com.trueprint.app.model

data class PageRange(val start: Int, val end: Int) {
    init {
        require(start > 0 && end >= start)
    }

    override fun toString(): String = if (start == end) "$start" else "$start-$end"
}

sealed interface PageSelection {
    data object All : PageSelection
    data class Custom(val specification: String, val ranges: List<PageRange>) : PageSelection
}

enum class DuplexMode(val ippValue: String, val label: String) {
    Off("one-sided", "Off"),
    LongEdge("two-sided-long-edge", "Long edge"),
    ShortEdge("two-sided-short-edge", "Short edge")
}

enum class PrintOrientation(val label: String) { Portrait("Portrait"), Landscape("Landscape") }

enum class PaperType(val cupsValue: String, val label: String) {
    Plain("PLAIN", "Plain Paper"),
    Thin("THIN", "Thin Paper"),
    Thick("THICK", "Thick Paper"),
    Thicker("THICKER", "Thicker Paper"),
    Bond("BOND", "Bond Paper"),
    Transparency("TRANS", "Transparency"),
    Envelope("ENV", "Envelope"),
    ThickEnvelope("ENV-THICK", "Thick Envelope"),
    ThinEnvelope("ENV-THIN", "Thin Envelope")
}

data class PrintOptions(
    val pageSelection: PageSelection = PageSelection.All,
    val pagesPerSheet: Int = 1,
    val duplex: DuplexMode = DuplexMode.Off,
    val orientation: PrintOrientation = PrintOrientation.Portrait,
    val paperType: PaperType = PaperType.Plain
) {
    init { require(pagesPerSheet in setOf(1, 2, 4)) }
}

/** Parses user-facing one-based page selections without silently correcting invalid input. */
object PageRangeParser {
    fun parse(specification: String, pageCount: Int? = null): Result<List<PageRange>> = runCatching {
        val value = specification.trim()
        require(value.isNotEmpty()) { "Enter one or more page numbers." }
        val ranges = value.split(',').map { part ->
            val token = part.trim()
            require(token.isNotEmpty()) { "Page selections cannot contain an empty item." }
            val match = Regex("^(\\d+)(?:-(\\d+))?$").matchEntire(token)
                ?: throw IllegalArgumentException("Use page numbers such as 1, 1-3, or 1,4-6.")
            val start = match.groupValues[1].toIntOrNull()
                ?: throw IllegalArgumentException("Page number is too large.")
            val end = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: start
            require(start > 0 && end > 0) { "Page numbers must start at 1." }
            require(end >= start) { "A page range must end on or after its start." }
            if (pageCount != null) require(end <= pageCount) {
                "This document has $pageCount page${if (pageCount == 1) "" else "s"}."
            }
            PageRange(start, end)
        }
        ranges
    }
}
