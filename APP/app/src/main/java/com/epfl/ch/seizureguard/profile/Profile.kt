// Profile.kt
package com.epfl.ch.seizureguard.profile

import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.seizure_event.SeizureEntity
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent

data class Profile(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var birthdate: String = "",
    var uri: String = "",
    var pwd: String = "",
    var epi_type: String = "Unknown",
    var auth_mode: String = "password",
    var isBiometricEnabled: Boolean = false,
    var isTrainingEnabled: Boolean = false,
    var isDebugEnabled: Boolean = false,
    var powerMode: String = "Normal",
    var emergencyContacts: List<EmergencyContact> = emptyList(),
    var pastSeizures: List<SeizureEvent> = emptyList(),
    var defaultsMetrics: Metrics = Metrics(),
    var latestMetrics: Metrics = defaultsMetrics,
    var medications: List<String> = emptyList()
) {
    companion object {
        fun isComplete(profile: Profile): Boolean {
            return with(profile) {
                name.isNotEmpty() && email.isNotEmpty() && birthdate.isNotEmpty() && pwd.isNotEmpty() && uri.isNotEmpty()
            }
        }
        fun empty() = Profile()
    }
}

data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val photoUri: String? = null
)
