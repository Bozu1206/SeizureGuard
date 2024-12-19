package com.epfl.ch.seizureguard.biometrics

enum class BiometricAuthStatus(val code: Int) {
    /**
     *  We can interact with the biometric support
     */
    READY(1),

    /**
     * Biometry support not present
     */
    NOT_AVAILABLE(-1),

    /**
     * Biometric support is currently unavailable
     */
    TEMPORARY_NOT_AVAILABLE(-2),

    /**
     * Biometric support is available, but no biometry has enrolled
     */
    AVAILABLE_BUT_NOT_ENROLLED(-3),
}