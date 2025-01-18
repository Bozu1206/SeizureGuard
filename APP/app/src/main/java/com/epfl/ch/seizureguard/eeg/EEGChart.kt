import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.epfl.ch.seizureguard.profile.ProfileViewModel


@Composable
fun EEGChart(
    isDebugEnabled : Boolean,
    isInferenceRunning : Boolean,
    eegViewModel: EEGViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val eegData by eegViewModel.eegData.collectAsState()
    val pointsToShow by eegViewModel.pointsToShow.collectAsState()
    val scrollOffset by eegViewModel.scrollOffset.collectAsState()
    val samplesPerChannel by eegViewModel.samplesPerChannel.collectAsState()

    val sampleWidthRatio : Float = if(isDebugEnabled) 4f else 16f

    var isDragging by remember { mutableStateOf(false) }
    var canvasWidth by remember { mutableStateOf(0f) }
    var isAutoScrolling by remember { mutableStateOf(true) }

    val leftMargin = 250f

    DisposableEffect(Unit) {
        eegViewModel.registerReceiver(context)
        onDispose {
            eegViewModel.unregisterReceiver(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            val graphWidth = canvasWidth - leftMargin
                            val totalSamples = if (eegData.isNotEmpty()) {
                                eegData[0].size / samplesPerChannel
                            } else 0
                            val sampleWidth = graphWidth / sampleWidthRatio
                            val totalWidth = totalSamples * sampleWidth

                            if (totalWidth > graphWidth) {
                                isDragging = true
                                isAutoScrolling = false
                            }
                        },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (isInferenceRunning && isDragging) {
                                val graphWidth = canvasWidth - leftMargin
                                eegViewModel.updateScrollOffset(dragAmount.x, graphWidth, sampleWidthRatio)
                            }
                        }
                    )
                }
        ) {
            canvasWidth = size.width
            val graphWidth = canvasWidth - leftMargin

            if (isInferenceRunning && !isDragging && isAutoScrolling && eegData.isNotEmpty()) {
                val totalSamples = eegData[0].size / samplesPerChannel
                val sampleWidth = graphWidth / sampleWidthRatio
                val safetyMargin = sampleWidth / 2
                val minScroll = -(totalSamples * sampleWidth) + graphWidth + safetyMargin
                
                if (scrollOffset > minScroll) {
                    eegViewModel.updateScrollOffset(minScroll, graphWidth, sampleWidthRatio)
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
                    if (isInferenceRunning && eegData.isNotEmpty()) {
                        eegData.forEachIndexed { index, channelData ->
                            val yCenter = (index + 1) * spacingY
                            drawChannel(
                                channelData.take(pointsToShow),
                                leftMargin,
                                yCenter,
                                graphWidth,
                                sampleWidthRatio,
                                spacingY,
                                samplesPerChannel,
                                channelData.size / samplesPerChannel
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
        color = android.graphics.Color.WHITE
        textSize = 50f
        isAntiAlias = true
        isFakeBoldText = true
    }

    channelNames.forEachIndexed { index, channelName ->
        val yCenter = (index + 1) * spacingY

        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(leftMargin, yCenter),
            end = Offset(leftMargin + graphWidth, yCenter),
            strokeWidth = 1.5f
        )

        drawContext.canvas.nativeCanvas.drawText(
            channelName,
            50f,
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
    sampleWidthRatio: Float,
    spacingY: Float,
    samplesPerChannel: Int,
    totalSamples: Int
) {
    if (channelData.isEmpty()) return
    
    val path = Path()
    val amplitudeY = spacingY * 1.2f
    val sampleWidth = graphWidth / sampleWidthRatio
    val totalWidth = sampleWidth * totalSamples
    val pixelsPerPoint = sampleWidth / samplesPerChannel
    
    path.moveTo(leftMargin, yCenter - channelData[0] * amplitudeY)
    
    for (i in channelData.indices) {
        val xPos = leftMargin + (i * pixelsPerPoint)
        val yPos = yCenter - channelData[i] * amplitudeY
        path.lineTo(xPos, yPos)
    }

    drawPath(
        path = path,
        color = Color.Green.copy(0.8f),
        style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
