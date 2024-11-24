package com.example.seizuregard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.seizuregard.dl.metrics.Metrics
import com.example.seizuregard.ui.theme.SeizuregardTheme

@Composable
fun ProfileScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter

    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Profile Section
            UserProfileSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Emergency Contacts Section
            EmergencyContactsSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Upload Medical History Button
            UploadMedicalHistorySection()
        }
    }
}

@Composable
fun UserProfileSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp)
    ) {
        // User Avatar
        Image(
            painter = painterResource(id = R.drawable.random),
            contentDescription = "User profile picture",
            modifier = Modifier
                .padding(top = 20.dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User Name
        Text(
            text = "François Dumoncel",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // User email
        Text(
            text = "francois.dumoncel@epfl.ch",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EmergencyContactsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Emergency Contacts",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        EmergencyContactCard(name = "Wife", phone = "+41 78 235 98 76")
        EmergencyContactCard(name = "Sister", phone = "+41 78 235 98 78")
        EmergencyContactCard(name = "Doctor Smith", phone = "+41 78 235 98 79")

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun EmergencyContactCard(name: String, phone: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
            .border(0.3.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun UploadMedicalHistorySection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(text = "Upload Medical History")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Uploading your medical history helps improve emergency response.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    SeizuregardTheme {
        ProfileScreen()
    }
}
