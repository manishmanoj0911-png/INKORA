package com.example.inkora.model

enum class PaperType(val displayName: String) {
    BLANK("Blank"),
    RULED("Ruled"),
    NARROW_RULED("Narrow Ruled"),
    WIDE_RULED("Wide Ruled"),
    GRID("Grid"),
    DOTTED("Dotted"),
    GRAPH("Graph Paper"),
    CORNELL("Cornell Notes"),
    CHECKLIST("Checklist Paper"),
    MUSIC("Music Staff")
}

data class PaperBackground(
    val type: PaperType = PaperType.RULED,
    val backgroundColorHex: String = "#FFFDF9",
    val lineColorHex: String = "#E2E8F0",
    val spacingDp: Float = 28f,
    val lineThicknessDp: Float = 1.0f,
    val dotRadiusDp: Float = 1.5f,
    val showMarginLine: Boolean = true,
    val marginOffsetDp: Float = 56f
) {
    companion object {
        val ClassicCream = PaperBackground(
            type = PaperType.RULED,
            backgroundColorHex = "#FFFDF5",
            lineColorHex = "#E5E0D8",
            spacingDp = 28f
        )
        val CrispWhiteGrid = PaperBackground(
            type = PaperType.GRID,
            backgroundColorHex = "#FFFFFF",
            lineColorHex = "#EDF2F7",
            spacingDp = 24f
        )
        val DotJournal = PaperBackground(
            type = PaperType.DOTTED,
            backgroundColorHex = "#FAFAF9",
            lineColorHex = "#CBD5E1",
            spacingDp = 24f
        )
        val CornellClassic = PaperBackground(
            type = PaperType.CORNELL,
            backgroundColorHex = "#FFFDF7",
            lineColorHex = "#CBD5E1",
            spacingDp = 26f
        )
        val DarkSlateGrid = PaperBackground(
            type = PaperType.GRID,
            backgroundColorHex = "#131720",
            lineColorHex = "#283042",
            spacingDp = 24f
        )
        val MidnightRuled = PaperBackground(
            type = PaperType.RULED,
            backgroundColorHex = "#0F1218",
            lineColorHex = "#242C3D",
            spacingDp = 28f
        )
        val SoftSage = PaperBackground(
            type = PaperType.RULED,
            backgroundColorHex = "#F2F7F4",
            lineColorHex = "#D0E1D4",
            spacingDp = 28f
        )
        val LavenderNotes = PaperBackground(
            type = PaperType.RULED,
            backgroundColorHex = "#F6F4FB",
            lineColorHex = "#DFD9EF",
            spacingDp = 28f
        )
    }
}
