import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

@Composable
fun MockEEGChart() {
    val channelNames = listOf(
        "FP1-F7",
        "F7-T7",
        "FP1-F3",
        "FZ-CZ",
        "FP2-F4",
        "FP2-F8"
    )

    val channelCount = channelNames.size
    val nbPoints = 50
    val fakeData = remember {
        List(channelCount) {
            List(nbPoints) { kotlin.random.Random.nextFloat() }
        }
    }

    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 55f
            isAntiAlias = true
        }
    }

    Canvas(
        modifier = Modifier
            .size(width = 380.dp, height = 270.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val leftMargin = 250f
        val graphWidth = canvasWidth - leftMargin

        val spacingY = canvasHeight / (channelCount + 1)

        val stepX = graphWidth / (nbPoints - 1)

        drawLine(
            color = Color.Gray,
            start = Offset(leftMargin, canvasHeight - 1),
            end = Offset(canvasWidth, canvasHeight - 1),
            strokeWidth = 2f
        )

        channelNames.forEachIndexed { index, channelName ->
            val yCenter = (index + 1) * spacingY

            drawContext.canvas.nativeCanvas.drawText(
                channelName,
                /* x = */ 35f,
                /* y = */ yCenter + (labelPaint.textSize / 3f),
                /* paint = */ labelPaint
            )

            val path = Path()
            val firstValue = fakeData[index].first()
            val amplitudeY = spacingY / 2.5f
            val x0 = leftMargin
            val y0 = yCenter - (firstValue - 0.5f) * amplitudeY
            path.moveTo(x0, y0)

            for (i in 1 until nbPoints) {
                val xPos = leftMargin + i * stepX
                val value = fakeData[index][i]
                val yPos = yCenter - (value - 0.5f) * amplitudeY
                path.lineTo(xPos, yPos)
            }

            drawPath(
                path = path,
                color = Color.Blue,
                style = Stroke(width = 8f)
            )
        }
    }
}
