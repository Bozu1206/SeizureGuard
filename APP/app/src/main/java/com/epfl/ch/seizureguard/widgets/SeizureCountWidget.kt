package com.epfl.ch.seizureguard.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.epfl.ch.seizureguard.MainActivity
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.Profile
import com.epfl.ch.seizureguard.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SeizureCountWidget : GlanceAppWidget() {
    companion object {
        suspend fun updateWidget(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(SeizureCountWidget::class.java)
                .forEach { glanceId ->
                    updateAppWidgetState(
                        context = context,
                        glanceId = glanceId
                    ) { }
                    SeizureCountWidget().update(context, glanceId)
                }
        }
    }

    private fun createGradientBitmap(
        width: Int,
        height: Int,
        startColor: Int,
        endColor: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            startColor, endColor,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val profile = withContext(Dispatchers.IO) {
            ProfileRepository.getInstance(context).loadProfileFromPreferences()
        }

        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(
                            ImageProvider(
                                createGradientBitmap(
                                    300,
                                    100,
                                    0xFFffa751.toInt(),
                                    0xFFffe259.toInt()
                                )
                            )
                        )
                        .cornerRadius(16.dp)
                        .padding(16.dp)
                        .clickable(onClick = actionStartActivity(mainActivityIntent)),
                ) {
                    Image(
                        provider = ImageProvider(R.mipmap.icon),
                        contentDescription = "App Icon",
                        modifier = GlanceModifier.size(38.dp)
                    )

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlanceText(
                            modifier = GlanceModifier.defaultWeight().padding(start = (-20).dp),
                            text = "Seizures",
                            font = R.font.sfr_medium,
                            fontSize = 20.sp,
                            color = Color.Black
                        )

                        GlanceText(
                            text = "${getSeizuresThisWeek(profile)}",
                            font = R.font.sfr_heavy,
                            fontSize = 28.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }

    private fun getSeizuresThisWeek(profile: Profile): Int {
        val weekInMillis = 7 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()
        return profile.pastSeizures.count { seizure ->
            seizure.timestamp > (currentTime - weekInMillis)
        }
    }
}

class SeizureCountWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SeizureCountWidget()
} 