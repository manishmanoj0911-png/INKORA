package com.example.inkora.model

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

enum class FontCategory(val label: String) {
    ALL("All Fonts"),
    SANS_SERIF("Sans Serif"),
    SERIF("Serif"),
    HANDWRITING("Handwriting"),
    MONOSPACE("Monospace / Coding"),
    DISPLAY("Display / Elegant"),
    STUDY("School / Study")
}

data class FontItem(
    val id: String,
    val name: String,
    val category: FontCategory,
    val fontFamily: FontFamily,
    val previewSample: String = "The quick brown fox jumps over the lazy dog"
)

object FontCatalog {
    val OutfitFamily = FontFamily(
        Font(R.font.outfit, FontWeight.Normal)
    )
    val CaveatFamily = FontFamily(
        Font(R.font.caveat, FontWeight.Normal)
    )
    val DancingScriptFamily = FontFamily(
        Font(R.font.dancing_script, FontWeight.Normal)
    )
    val PlayfairFamily = FontFamily(
        Font(R.font.playfair_display, FontWeight.Normal)
    )
    val LoraFamily = FontFamily(
        Font(R.font.lora, FontWeight.Normal)
    )
    val FiraCodeFamily = FontFamily(
        Font(R.font.fira_code, FontWeight.Normal)
    )

    val fonts: List<FontItem> = listOf(
        FontItem("outfit", "Outfit", FontCategory.SANS_SERIF, OutfitFamily, "Clean, modern sans-serif for everyday writing"),
        FontItem("sans_system", "System Sans", FontCategory.SANS_SERIF, FontFamily.SansSerif, "Default system sans-serif font"),
        FontItem("lora", "Lora", FontCategory.SERIF, LoraFamily, "Contemporary serif with brushed calligraphy curves"),
        FontItem("playfair", "Playfair Display", FontCategory.DISPLAY, PlayfairFamily, "Sophisticated high-contrast editorial serif"),
        FontItem("caveat", "Caveat", FontCategory.HANDWRITING, CaveatFamily, "Friendly, open handwriting with warm personality"),
        FontItem("dancing_script", "Dancing Script", FontCategory.HANDWRITING, DancingScriptFamily, "Lively casual script with bouncing letters"),
        FontItem("fira_code", "Fira Code", FontCategory.MONOSPACE, FiraCodeFamily, "True monospace font optimized for code and technical notes"),
        FontItem("mono_system", "System Monospace", FontCategory.MONOSPACE, FontFamily.Monospace, "Clean fixed-pitch system font"),
        FontItem("study_lora", "Editorial Study", FontCategory.STUDY, LoraFamily, "Designed for long-form study guides and book notes"),
        FontItem("minimal_sans", "Minimalist Note", FontCategory.SANS_SERIF, OutfitFamily, "Spacious modern letterforms")
    )

    fun getFontById(id: String): FontItem {
        return fonts.find { it.id == id } ?: fonts.first()
    }
}
