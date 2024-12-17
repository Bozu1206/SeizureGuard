package com.example.seizureguard.history

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seizureguard.profile.convertMillisToDate
import com.example.seizureguard.seizure_event.DefaultState
import com.example.seizureguard.seizure_event.LogSeizureEventModal
import com.example.seizureguard.seizure_event.SeizureEvent

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier, historyViewModel: HistoryViewModel = viewModel()
) {
    var shouldShowEditDialog by remember { mutableStateOf(false) }
    var pastSeizure by remember { mutableStateOf<SeizureEvent?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        val seizures by historyViewModel.seizures.observeAsState()

        LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
            historyViewModel.readHistory()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "History", modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineLarge
            )

            seizures?.let {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(it) { seizure ->
                        SeizureItemRow(seizureEvent = seizure,
                            onEdit = {
                                Log.d("HistoryScreen", "Editing seizure")
                                shouldShowEditDialog = true
                                pastSeizure = seizure
                            },
                            onDelete = {
                                Log.d("HistoryScreen", "Removing seizure")
                                historyViewModel.removeSeizure(seizure)
                            })
                    }
                }
            }

            if (shouldShowEditDialog) {
                EditSeizure(onDismiss = { shouldShowEditDialog = false }, pastSeizure = pastSeizure!!, historyViewModel = historyViewModel)
            }
        }
    }
}

@Composable
fun EditSeizure(onDismiss: () -> Unit, pastSeizure: SeizureEvent, historyViewModel: HistoryViewModel) {
    LogSeizureEventModal(
        onDismiss = onDismiss,
        onClick = { seizureEvent ->
            historyViewModel.editSeizure(seizureEvent, pastSeizure)
            onDismiss()
        },
        label = "Edit Seizure",
        default = DefaultState(
            type = pastSeizure.type,
            duration = pastSeizure.duration,
            severity = pastSeizure.severity,
            triggers = pastSeizure.triggers
        )
    )
}

@Composable
fun SeizureItemRow(
    seizureEvent: SeizureEvent,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val severityColors: List<Color> = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFFF9800), // Orange
        Color(0xFFF44336)  // Red
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = severityColors[seizureEvent.severity - 1].copy(0.1f),
            contentColor = Color.Black
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(16.dp)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = seizureEvent.type + " Seizure",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = Color.Black.copy(0.5f),
                    modifier = Modifier.clickable {
                        onEdit()
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.RemoveCircle,
                    contentDescription = null,
                    tint = Color.Black.copy(0.5f),
                    modifier = Modifier.clickable {
                        onDelete()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = convertMillisToDate(seizureEvent.timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(0.8f),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${seizureEvent.duration} minutes long",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(0.8f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = (-4).dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        seizureEvent.triggers.forEach { trigger ->
                            Pill(text = trigger, color = severityColors[seizureEvent.severity - 1])
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Pill(text: String, color: Color) {
    Text(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .background(color.copy(0.15f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .wrapContentHeight()
            .wrapContentWidth()
    )

}

@Preview
@Composable
fun PreviewHistoryScreen() {
    HistoryScreen()
}

