package com.example.seizureguard.wallet_manager

interface GoogleWalletToken {
    data class PassRequest(
        val patientName: String,
        val emergencyContact: String,
        val seizureType: String,
        val medication: String,
        val birthdate: String
    )
}