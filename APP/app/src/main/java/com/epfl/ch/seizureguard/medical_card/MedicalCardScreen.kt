package com.epfl.ch.seizureguard.medical_card

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.seizureguard.wallet_manager.GoogleWalletToken

@Composable
fun MedicalCardScreen(
    payState: WalletUiState,
    requestSavePass: (GoogleWalletToken.PassRequest) -> Unit
) {
    when (payState) {
        is WalletUiState.PassAdded -> AddToGoogleWalletSuccess()
        is WalletUiState.Available -> MedicalCardForm { requestSavePass(it) }
        else -> IndeterminateCircularIndicator()
    }
}


@Composable
fun AddToGoogleWalletSuccess() = Column(
    modifier = Modifier
        .padding(20.dp)
        .fillMaxWidth()
        .fillMaxHeight(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Toast.makeText(
        LocalContext.current,
        "Medical card is already in Google Wallet",
        Toast.LENGTH_SHORT
    ).show()
}

@Composable
fun IndeterminateCircularIndicator() = CircularProgressIndicator(
    color = MaterialTheme.colorScheme.surfaceVariant,
    trackColor = MaterialTheme.colorScheme.secondary,
)