package com.epfl.ch.seizureguard.medical_space

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalSpaceScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel,
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.medical_space_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Medical Space Features Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MedicalFeatureCard(
                    title = stringResource(R.string.seizure_history),
                    icon = Icons.Default.History,
                    gradient = Brush.linearGradient(
                        listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("seizure_history")
                }
                
                MedicalFeatureCard(
                    title = stringResource(R.string.statistics),
                    icon = Icons.Default.ShowChart,
                    gradient = Brush.linearGradient(
                        listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("seizure_stats")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MedicalFeatureCard(
                    title = stringResource(R.string.medication_tracker),
                    icon = Icons.Default.MedicalServices,
                    gradient = Brush.linearGradient(
                        listOf(Color(0xFF2196F3), Color(0xFF1976D2))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("medication_tracker")
                }

                MedicalFeatureCard(
                    title = stringResource(R.string.medical_notes),
                    icon = Icons.Default.Notes,
                    gradient = Brush.linearGradient(
                        listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("medical_notes")
                }
            }

            // Add space image at the bottom
            Image(
                painter = painterResource(id = R.drawable.spaces),
                contentDescription = "Space illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun MedicalFeatureCard(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Background icon (larger and faded)
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .alpha(0.15f),
                tint = Color.White
            )

            // Content layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon in normal size
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )

                // Title at the bottom
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} 