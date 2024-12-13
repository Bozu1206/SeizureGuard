package com.example.seizureguard.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seizureguard.seizure_event.SeizureEvent

@Composable
fun HistoryScreen(modifier: Modifier = Modifier, historyViewModel: HistoryViewModel = viewModel()
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val seizures by historyViewModel.seizures.observeAsState()
        LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {// at onResume (when we open this screen) we look for past sessions
            historyViewModel.readHistory()
        }
        seizures?.let { // if sessions non-null
            LazyColumn { // lazy column listing all sessions
                items(it) { seizure->
                    SeizureItemRow(seizureEvent = seizure, Modifier.padding(8.dp)) // lazy column entry
                }
            }
        }
    }
}

@Composable
fun SeizureItemRow(seizureEvent: SeizureEvent, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier.padding(8.dp)) {
            Text(
                text = seizureEvent.type ?: "",
                style = TextStyle(fontSize = 20.sp, color = Color.Black)
            )
            Text(text = seizureEvent.duration.toString() ?: "")
            Text(text = seizureEvent.severity.toString() ?: "")
            seizureEvent.triggers.forEach { trigger ->
                Text(text = trigger ?: "")
            }
        }
    }
}