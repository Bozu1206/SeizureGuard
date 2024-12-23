// Profile.kt
package com.epfl.ch.seizureguard.profile

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
    var emergencyContacts: List<EmergencyContact> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "birthdate" to birthdate,
            "uri" to uri,
            "pwd" to pwd,
            "epi_type" to epi_type,
            "auth_mode" to auth_mode,
            "isBiometricEnabled" to isBiometricEnabled,
            "isTrainingEnabled" to isTrainingEnabled,
            "emergencyContacts" to emergencyContacts
        )
    }

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
