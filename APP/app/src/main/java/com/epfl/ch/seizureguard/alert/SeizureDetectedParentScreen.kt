package com.epfl.ch.seizureguard.alert

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.epfl.ch.seizureguard.seizure_event.LogSeizureEventModal
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeizureDetectedParentScreen(
    latitude : Double?,
    longitude: Double?,
    onDismiss: () -> Unit,
    onEmergencyCall: () -> Unit,
    profileViewModel: ProfileViewModel,
    context: Context
) {
    var isLogging by remember { mutableStateOf(false) }
    var hasLogged by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf<String?>(null) }

    // Fetch the address if latitude and longitude are not null
    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            address = try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("SeizureDetectedScreen", "Geocoding error: ${e.message}")
                null
            }
        }
    }

    // Dark semi-transparent background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(enabled = false) {}
    )

    // Main content background
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
                text = stringResource(R.string.seizure_detected_parent),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(32.dp))
            if (latitude != null && longitude != null && !latitude.isNaN() && !longitude.isNaN()) {
                // Show map if location is available
                val mapCenter = LatLng(latitude, longitude)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(mapCenter, 15f)
                }
                val markerState = remember { MarkerState(position = mapCenter) }
                GoogleMap(
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = MapType.HYBRID),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .fillMaxWidth()
                        .size(150.dp)
                ) {
                    Marker(
                        state = markerState,
                        title = stringResource(R.string.marker_title_text),
                        snippet = address
                            ?: "Latitude: ${mapCenter.latitude}, Longitude: ${mapCenter.longitude}"
                    )
                }
            } else {
                // No location available, show image
                Image(
                    painter = painterResource(R.drawable.seizure_alert),
                    contentDescription = null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .fillMaxWidth()
                        .size(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(128.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 250.dp)
        ) {
            if (address != null) {
                Text(
                    text = stringResource(R.string.address_prefix, address!!),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Already localized guidelines in your strings.xml
            Text(
                text = stringResource(id = R.string.guidelines),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }

        SeizureAlertButtonsParent(
            onEmergencyCall = onEmergencyCall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        if (isLogging) {
            LogSeizureEventModal(
                onDismiss = { isLogging = false },
                onClick = { seizureEvent ->
                    hasLogged = true
                    profileViewModel.addSeizure(seizureEvent)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun SeizureAlertButtonsParent(
    onEmergencyCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Localize "Call Emergency"
    val callEmergencyText = stringResource(R.string.call_emergency_text)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        AlertButton(
            text = callEmergencyText,
            icon = Icons.Default.Call,
            onClick = onEmergencyCall,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
