/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
 Borrowed from
    https://github.com/google-wallet/android-sample/blob/main/kotlin/app/src/main/java/com/google/android/gms/samples/wallet/viewmodel/WalletViewModel.kt
 */

package com.epfl.ch.seizureguard.medical_card

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.pay.Pay
import com.google.android.gms.pay.PayApiAvailabilityStatus
import com.google.android.gms.pay.PayClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val _walletUiState = MutableStateFlow<WalletUiState>(WalletUiState.Available)
    val walletUiState: StateFlow<WalletUiState> = _walletUiState.asStateFlow()

    // A client to interact with the Google Wallet API
    private val walletClient: PayClient = Pay.getClient(application)

    init {
        viewModelScope.launch {
            fetchCanAddPassesToGoogleWallet()
        }
    }

    /**
     * Determine whether the API to save passes to Google Pay is available on the device.
     */
    private suspend fun fetchCanAddPassesToGoogleWallet() {
        val status = walletClient
            .getPayApiAvailabilityStatus(PayClient.RequestType.SAVE_PASSES).await()

        val newState = when(status) {
            PayApiAvailabilityStatus.AVAILABLE -> WalletUiState.Available
            else -> WalletUiState.Error(
                CommonStatusCodes.ERROR,
                "Google Wallet is not available in this device."
            )
        }

        _walletUiState.update { newState }
    }

    /**
     * Exposes the `savePassesJwt` method in the wallet client
     */
    val savePassesJwt: (String, Activity, Int) -> Unit = walletClient::savePassesJwt
}


abstract class WalletUiState internal constructor(){
    object Unknown : WalletUiState()
    object Available : WalletUiState()
    class PassAdded : WalletUiState()
    class Error(val code: Int, val message: String? = null) : WalletUiState()
}