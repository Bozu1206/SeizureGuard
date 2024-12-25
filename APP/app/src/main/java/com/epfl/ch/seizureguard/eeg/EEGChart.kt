package com.epfl.ch.seizureguard.eeg

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun EEGChart(
    channels: List<List<Float>>,   // 6 canaux, 1024 points chacun
    modifier: Modifier = Modifier
) {
    // S'il n'y a pas de canaux, on ne dessine rien
    if (channels.isEmpty()) return

    val numberOfChannels = channels.size

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // On peut mettre une marge “globale” en haut et en bas
        val marginTop = 20f
        val marginBottom = 20f
        // Espace total pour les 6 tracés
        val usableHeight = h - marginTop - marginBottom

        // Distance verticale entre lignes (bandes)
        // On divise l’espace en numberOfChannels
        val offset = usableHeight / numberOfChannels

        // Nombre de points par canal
        val nbPoints = channels.firstOrNull()?.size?.coerceAtLeast(2) ?: 2
        val stepX = w / (nbPoints - 1)

        // Pour chaque canal
        channels.forEachIndexed { index, channelData ->
            val path = Path()

            // Valeurs min & max pour normaliser
            val minVal = channelData.minOrNull() ?: 0f
            val maxVal = channelData.maxOrNull() ?: 1f
            val amplitude = (maxVal - minVal).coerceAtLeast(1e-6f)

            // On va dessiner la courbe dans (offset - 10) de hauteur environ,
            // pour laisser un peu de marge en haut/bas de la bande
            val bandHeight = offset - 10f
            val scaleY = bandHeight / amplitude

            // baseline = la ligne centrale de la bande
            // index * offset = on avance vers le bas canal par canal
            // + marginTop = on laisse l'espace au début
            // + offset/2 -> on place la courbe au milieu de la “bande”
            val baseline = marginTop + index * offset + (offset / 2f)

            // Premier point
            val y0 = baseline - (channelData[0] - minVal) * scaleY
            path.moveTo(0f, y0)

            // Construire la courbe
            for (i in 1 until nbPoints) {
                val x = i * stepX
                val yRaw = channelData[i]
                val yMapped = baseline - (yRaw - minVal) * scaleY
                path.lineTo(x, yMapped)
            }

            // Couleur distincte pour chaque canal
            val color = Color.hsl(
                hue = (360f / numberOfChannels) * index,
                saturation = 0.7f,
                lightness = 0.5f
            )

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2f)
            )
        }
    }
}
