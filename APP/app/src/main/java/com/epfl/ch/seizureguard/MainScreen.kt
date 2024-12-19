package com.example.seizureguard

import MetricsViewModel
import ProfileViewModel
import androidx.compose.runtime.Composable
import com.example.seizureguard.dl.metrics.Metrics
import com.example.seizureguard.history.HistoryViewModel
import com.example.seizureguard.navigation.AppContent
import com.example.seizureguard.seizure_event.SeizureEventViewModel
import com.example.seizureguard.wallet_manager.GoogleWalletToken
import com.google.android.gms.samples.wallet.viewmodel.WalletUiState

@Composable
fun MainScreen(
    onRunInference: () -> Unit,
    metrics: Metrics,
    payState: WalletUiState,
    profileViewModel: ProfileViewModel,
    historyViewModel: HistoryViewModel,
    seizureEventViewModel: SeizureEventViewModel,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit,
    metricsViewModel: MetricsViewModel
) {
    AppContent(
        metrics = metrics,
        onRunInference = onRunInference,
        payState = payState,
        requestSavePass = requestSavePass,
        profileViewModel = profileViewModel,
        seizureEventViewModel = seizureEventViewModel,
        historyViewModel = historyViewModel,
        metricsViewModel = metricsViewModel
    )
}
