package com.epfl.ch.seizureguard.wallet_manager

interface GoogleWalletToken {
    data class PassRequest(
        val uid: String,
        val patientName: String,
        val emergencyContact: String,
        val seizureType: String,
        val medication: String,
        val birthdate: String
    )
}