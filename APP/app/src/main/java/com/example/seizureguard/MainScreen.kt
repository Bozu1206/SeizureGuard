package com.example.seizureguard

import ProfileViewModel
import androidx.compose.runtime.Composable
import com.example.seizureguard.alert.SeizureDetectedScreen
import com.example.seizureguard.wallet_manager.GoogleWalletToken
import com.example.seizureguard.dl.metrics.Metrics
import com.example.seizureguard.navigation.AppContent
import com.example.seizureguard.ui.theme.AppTheme
import com.google.android.gms.samples.wallet.viewmodel.WalletUiState

@Composable
fun MainScreen(
    isSeizureDetected: Boolean,
    onDismissSeizure: () -> Unit,
    onEmergencyCall: () -> Unit,
    onRunInference: () -> Unit,
    metrics: Metrics,
    payState: WalletUiState,
    profileViewModel: ProfileViewModel,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit
) {
    AppTheme {
        if (isSeizureDetected) {
            SeizureDetectedScreen(onDismiss = onDismissSeizure, onEmergencyCall = onEmergencyCall)
        } else {
            AppContent(
                metrics = metrics,
                onRunInference = onRunInference,
                payState = payState,
                requestSavePass = requestSavePass,
                profileViewModel = profileViewModel
            )
        }
    }
}
