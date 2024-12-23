// ProfileViewModel.kt
package com.epfl.ch.seizureguard.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val _profileState = MutableStateFlow(Profile.empty())
    val profileState: StateFlow<Profile> = _profileState

    val isAuthenticated: StateFlow<Boolean> = repository.context.dataStore.data
        .map { preferences -> preferences[Keys.IS_AUTHENTICATED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadProfile()
    }

    private fun loadProfile() = viewModelScope.launch {
        val profile = repository.loadProfileFromPreferences()
        _profileState.value = profile
        Log.d("ProfileViewModel", "Loaded profile: $profile")
    }

    fun loadProfileFromEmail(email: String, password: String, onLoggedIn: () -> Unit) {
        viewModelScope.launch {
            try {
                val profile = repository.loadProfileFromFirestore(email, password)
                if (profile != null) {
                    _profileState.value = profile
                    repository.saveProfileToPreferences(profile)
                    setAuthenticated(true)
                    onLoggedIn()
                    Log.d("ProfileViewModel", "Profile loaded and saved: $profile")
                } else {
                    Log.d("ProfileViewModel", "No matching profile found for email: $email")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to load profile from Firestore", e)
            }
        }
    }

    fun saveProfile() = viewModelScope.launch {
        val profile = _profileState.value
        Log.d("ProfileViewModel", "Saving profile: $profile")
        repository.saveProfileToPreferences(profile)
        repository.saveProfileToFirestore(profile)
        Log.d("ProfileViewModel", "Profile saved successfully.")
    }

    fun setAuthenticated(authenticated: Boolean) = viewModelScope.launch {
        repository.setAuthenticated(authenticated)
        Log.d("ProfileViewModel", "Set authenticated: $authenticated")
    }

    fun saveAuthPreference(isBiometric: Boolean) {
        viewModelScope.launch {
            repository.saveAuthPreference(isBiometric)
            _profileState.update { currentState ->
                currentState.copy(
                    auth_mode = if (isBiometric) "biometric" else "password",
                    isBiometricEnabled = isBiometric
                )
            }
            saveProfile() // Persiste les changements
            Log.d("ProfileViewModel", "Saved auth preference: isBiometric=$isBiometric")
        }
    }

    fun resetProfile() {
        viewModelScope.launch {
            repository.resetPreferences()
            _profileState.value = Profile.empty()
            Log.d("ProfileViewModel", "Profile reset.")
        }
    }

    fun updateProfileField(key: String, value: String) {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "Updating profile field: $key with value: $value")
            repository.updateProfileField(key, value) // Met à jour dans DataStore

            // Met à jour l'état local
            _profileState.update { currentProfile ->
                when (key) {
                    "name" -> currentProfile.copy(name = value)
                    "email" -> currentProfile.copy(email = value)
                    "birthdate" -> currentProfile.copy(birthdate = value)
                    "uri" -> currentProfile.copy(uri = value)
                    "pwd" -> currentProfile.copy(pwd = value)
                    "epi_type" -> currentProfile.copy(epi_type = value)
                    "auth_mode" -> currentProfile.copy(auth_mode = value)
                    else -> currentProfile
                }
            }
            Log.d("ProfileViewModel", "Updated _profileState: ${_profileState.value}")
            saveProfile() // Persiste les changements
        }
    }

    fun updateMultipleFieldsAndSave(map: Map<String, String>) {
        viewModelScope.launch {
            _profileState.update { current ->
                current.copy(
                    name = map["name"] ?: current.name,
                    email = map["email"] ?: current.email,
                    epi_type = map["epi_type"] ?: current.epi_type,
                )
            }

            val updatedProfile = _profileState.value
            Log.d("ProfileViewModel", "Batch updating and saving profile: $updatedProfile")

            repository.saveProfileToPreferences(updatedProfile)
            repository.saveProfileToFirestore(updatedProfile)
            Log.d("ProfileViewModel", "Batch save completed.")
        }
    }

    fun saveTrainingPreference(isEnabled: Boolean) {
        viewModelScope.launch {
            repository.saveTrainingPreference(isEnabled)
            _profileState.update { currentProfile ->
                currentProfile.copy(isTrainingEnabled = isEnabled)
            }
            saveProfile() // Persiste les changements
            Log.d("ProfileViewModel", "Saved training preference: isEnabled=$isEnabled")
        }
    }

    fun updateEmergencyContacts(contact: EmergencyContact, isAdding: Boolean) = viewModelScope.launch {
        val updatedContacts = if (isAdding) {
            if (_profileState.value.emergencyContacts.size < 5) _profileState.value.emergencyContacts + contact else _profileState.value.emergencyContacts
        } else {
            _profileState.value.emergencyContacts.filter { it.phone != contact.phone }
        }
        _profileState.update { currentProfile ->
            currentProfile.copy(emergencyContacts = updatedContacts)
        }
        Log.d("ProfileViewModel", "Updated emergencyContacts: $updatedContacts")
        saveProfile() // Persiste les changements
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val repository = ProfileRepository(context)
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}