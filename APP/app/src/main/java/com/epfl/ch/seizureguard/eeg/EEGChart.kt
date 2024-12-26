import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epfl.ch.seizureguard.eeg.EEGViewModel


@Composable
fun EEGChart(
    viewModel: EEGViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val eegData by viewModel.eegData.collectAsState()
    val pointsToShow by viewModel.pointsToShow.collectAsState()
    val scrollOffset by viewModel.scrollOffset.collectAsState()
    
    var isDragging by remember { mutableStateOf(false) }
    var canvasWidth by remember { mutableStateOf(0f) }
    var isAutoScrolling by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        viewModel.registerReceiver(context)
        onDispose {
            viewModel.unregisterReceiver(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { 
                            isDragging = true
                            isAutoScrolling = false
                        },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val graphWidth = canvasWidth - 300f
                            viewModel.updateScrollOffset(dragAmount.x, graphWidth)
                        }
                    )
                }
        ) {
            canvasWidth = size.width
            val leftMargin = 300f
            val graphWidth = canvasWidth - leftMargin

            if (!isDragging && isAutoScrolling && eegData.isNotEmpty()) {
                val totalSamples = eegData[0].size / 1024
                val sampleWidth = graphWidth / 4f
                val safetyMargin = sampleWidth / 2
                val minScroll = -(totalSamples * sampleWidth) + graphWidth + safetyMargin
                
                if (scrollOffset > minScroll) {
                    viewModel.updateScrollOffset(minScroll, graphWidth)
                }
            }

            val canvasHeight = size.height
            val spacingY = canvasHeight / 7f

            val channelNames = listOf(
                "FP1-F7", "F7-T7", "FP1-F3",
                "FZ-CZ", "FP2-F4", "FP2-F8"
            )

            drawLabelsAndGrid(leftMargin, graphWidth, canvasHeight, spacingY, channelNames)

            clipRect(
                left = leftMargin,
                top = 0f,
                right = canvasWidth,
                bottom = canvasHeight
            ) {
                translate(left = scrollOffset) {
                    if (eegData.isNotEmpty()) {
                        eegData.forEachIndexed { index, channelData ->
                            val yCenter = (index + 1) * spacingY
                            drawChannel(
                                channelData.take(pointsToShow),
                                leftMargin,
                                yCenter,
                                graphWidth,
                                spacingY,
                                channelData.size / 1024
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLabelsAndGrid(
    leftMargin: Float,
    graphWidth: Float,
    canvasHeight: Float,
    spacingY: Float,
    channelNames: List<String>
) {
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 50f
        isAntiAlias = true
        isFakeBoldText = true
    }

    channelNames.forEachIndexed { index, channelName ->
        val yCenter = (index + 1) * spacingY

        drawLine(
            color = Color.Black.copy(alpha = 0.5f),
            start = Offset(leftMargin, yCenter),
            end = Offset(leftMargin + graphWidth, yCenter),
            strokeWidth = 1.5f
        )

        drawContext.canvas.nativeCanvas.drawText(
            channelName,
            100f,
            yCenter + (labelPaint.textSize / 3f),
            labelPaint
        )
    }
}

private fun DrawScope.drawChannel(
    channelData: List<Float>,
    leftMargin: Float,
    yCenter: Float,
    graphWidth: Float,
    spacingY: Float,
    totalSamples: Int
) {
    if (channelData.isEmpty()) return
    
    val path = Path()
    val amplitudeY = spacingY * 1.2f
    val sampleWidth = graphWidth / 4f
    val totalWidth = sampleWidth * totalSamples
    val pixelsPerPoint = sampleWidth / 1024f
    
    path.moveTo(leftMargin, yCenter - channelData[0] * amplitudeY)
    
    for (i in channelData.indices) {
        val xPos = leftMargin + (i * pixelsPerPoint)
        val yPos = yCenter - channelData[i] * amplitudeY
        path.lineTo(xPos, yPos)
    }

    drawPath(
        path = path,
        color = Color.Blue.copy(0.8f),
        style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
