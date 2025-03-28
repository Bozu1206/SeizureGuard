package com.epfl.ch.seizureguard.alert

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.glance.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.LogSeizureEventModal

@Composable
fun SeizureDetectedScreen(
    onDismiss: () -> Unit,
    onEmergencyCall: () -> Unit,
    profileViewModel: ProfileViewModel?,
    context: Context
) {
    var isLogging by remember { mutableStateOf(false) }
    var hasLogged by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(enabled = false) {}
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.errorContainer
                    .copy(alpha = 0.98f)
            )
            .clickable(enabled = false) {}
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.seizure_detected),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(R.drawable.seizure_alert),
                contentDescription = null,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .fillMaxWidth()
                    .size(200.dp)
            )

            Spacer(modifier = Modifier.height(128.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 250.dp)
        ) {

            Text(
                text = stringResource(id = R.string.guidelines),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }

        SeizureAlertButtons(
            onEmergencyCall = onEmergencyCall,
            onLogSeizure = { isLogging = true },
            onSeizureTerminated = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        if (isLogging) {
            LogSeizureEventModal(
                context = context,
                onDismiss = { isLogging = false },
                onClick = { seizureEvent ->
                    hasLogged = true
                    profileViewModel?.addSeizure(seizureEvent)
                    onDismiss()
            })
        }
    }
}

@Composable
fun SeizureAlertButtons(
    onEmergencyCall: () -> Unit,
    onLogSeizure: () -> Unit,
    onSeizureTerminated: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        AlertButton(
            text = "Call Emergency",
            icon = Icons.Default.Call,
            onClick = onEmergencyCall,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AlertButton(
                text = "Log Seizure",
                icon = Icons.Default.Add,
                onClick = onLogSeizure,
                colors = ButtonDefaults.buttonColors(Color(0xFF339933)),
                modifier = Modifier.weight(0.8f)
            )
            ButtonWithIcon(
                text = "Seizure Terminated",
                icon = Icons.Default.Check,
                onClick = onSeizureTerminated,
                colors = ButtonDefaults.buttonColors(Color(0xFF121212)),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ButtonWithIcon(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    colors: ButtonColors,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onError
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

@Composable
fun AlertButton(
    text: String,
    icon: ImageVector,
    colors: ButtonColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ButtonWithIcon(
        text = text,
        icon = icon,
        onClick = onClick,
        colors = colors,
        modifier = modifier
    )
}

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun SeizureDetectedScreenPreview() {
    SeizureDetectedScreen(onDismiss = {}, onEmergencyCall = {}, null, LocalContext.current)
}
