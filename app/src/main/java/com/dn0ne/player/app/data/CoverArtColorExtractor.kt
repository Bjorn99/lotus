package com.dn0ne.player.app.data

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.kmpalette.color
import com.kmpalette.generatePalette
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.ktx.toHct

class CoverArtColorExtractor(
    private val contentResolver: ContentResolver,
) {
    suspend fun extractDominantColor(coverArtUri: Uri): Int? {
        return try {
            val bitmap = contentResolver.openInputStream(coverArtUri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return null

            selectDominantColor(bitmap.asImageBitmap().generatePalette()).toArgb()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun selectOnColor(color: Int): Color {
            return if (Color(color).luminance() < 0.5f) Color.White else Color.Black
        }

        fun selectDominantColor(palette: Palette): Color {
            val swatches = palette.swatches.sortedByDescending { it.population }
            if (swatches.isEmpty()) return Color.Unspecified

            val firstSwatch = swatches.first()
            val firstSwatchColorHct = firstSwatch.color.toHct()
            val firstSwatchPopulation = firstSwatch.population
            val moreChromatic = swatches.firstOrNull {
                it.color.toHct().chroma - firstSwatchColorHct.chroma >= 30 &&
                    it.population.toFloat() / firstSwatchPopulation >= .1f
            }

            return moreChromatic?.color ?: firstSwatch.color
        }
    }
}
