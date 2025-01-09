package com.epfl.ch.seizureguard

import androidx.compose.runtime.Composable
import com.epfl.ch.seizureguard.dl.MetricsViewModel
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.history.HistoryViewModel
import com.epfl.ch.seizureguard.navigation.AppContent
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.wallet_manager.GoogleWalletToken
import com.epfl.ch.seizureguard.medical_card.WalletUiState
import com.epfl.ch.seizureguard.profile.ProfileViewModel

@Composable
fun MainScreen(
    onRunInference: () -> Unit,
    onPauseInference: () -> Unit,
    metrics: Metrics,
    payState: WalletUiState,
    profileViewModel: ProfileViewModel,
    historyViewModel: HistoryViewModel,
    seizureEventViewModel: SeizureEventViewModel,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit,
    metricsViewModel: MetricsViewModel,
    onLogoutClicked: () -> Unit
) {
    AppContent(
        metrics = metrics,
        onRunInference = onRunInference,
        onPauseInference = onPauseInference,
        payState = payState,
        requestSavePass = requestSavePass,
        profileViewModel = profileViewModel,
        seizureEventViewModel = seizureEventViewModel,
        historyViewModel = historyViewModel,
        metricsViewModel = metricsViewModel,
        onLogoutClicked = onLogoutClicked
    )
}
