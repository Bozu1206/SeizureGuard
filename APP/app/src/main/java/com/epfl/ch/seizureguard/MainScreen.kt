package com.epfl.ch.seizureguard

import androidx.compose.runtime.Composable
import com.epfl.ch.seizureguard.dl.MetricsViewModel
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.history.HistoryViewModel
import com.epfl.ch.seizureguard.navigation.AppContent
import com.epfl.ch.seizureguard.seizure_event.SeizureEventViewModel
import com.epfl.ch.seizureguard.wallet_manager.GoogleWalletToken
import com.epfl.ch.seizureguard.medical_card.WalletUiState
import com.epfl.ch.seizureguard.medical_card.WalletViewModel
import com.epfl.ch.seizureguard.profile.ProfileViewModel

@Composable
fun MainScreen(
    onRunInference: () -> Unit,
    onPauseInference: () -> Unit,
    payState: WalletUiState,
    profileViewModel: ProfileViewModel,
    walletViewModel: WalletViewModel,
    seizureEventViewModel: SeizureEventViewModel,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit,
    metricsViewModel: MetricsViewModel,
    onLogoutClicked: () -> Unit
) {
    AppContent(
        onRunInference = onRunInference,
        onPauseInference = onPauseInference,
        payState = payState,
        requestSavePass = requestSavePass,
        profileViewModel = profileViewModel,
        seizureEventViewModel = seizureEventViewModel,
        metricsViewModel = metricsViewModel,
        walletViewModel = walletViewModel,
        onLogoutClicked = onLogoutClicked
    )
}
