// ProfileViewModel.kt
package com.epfl.ch.seizureguard.profile

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epfl.ch.seizureguard.dl.ModelManager
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.inference.InferenceService
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.google.gson.GsonBuilder
import android.widget.Toast
import android.app.Activity
import android.net.Uri

class ProfileViewModel(context: Context, application: Application) : AndroidViewModel(application) {
    private val repository: ProfileRepository by lazy {
        ProfileRepository.getInstance(
            context = context
        )
    }

    val sampleCount: StateFlow<Int> = repository.sampleCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val isTrainReady: StateFlow<Boolean> = repository.isTrainReady
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val latestMetrics: StateFlow<Metrics> = repository.latestMetrics
        .stateIn(viewModelScope, SharingStarted.Eagerly, Metrics())

    fun requestTraining() {
        val intent = Intent(getApplication(), InferenceService::class.java).apply {
            action = "ACTION_START_TRAINING"
        }

        getApplication<Application>().startService(intent)
    }

    fun updateMetricsFromUI(metrics: Metrics) {
        repository.updateMetrics(metrics)
    }

    private val _profileState = MutableStateFlow(Profile.empty())
    val profileState: StateFlow<Profile> = _profileState

    val isAuthenticated: StateFlow<Boolean> = repository.context.dataStore.data
        .map { preferences -> preferences[Keys.IS_AUTHENTICATED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var pendingJsonExport: String? = null

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
            saveProfile()
            Log.d("ProfileViewModel", "Saved auth preference: isBiometric=$isBiometric")
        }
    }

    fun addSeizure(seizure: SeizureEvent) {
        viewModelScope.launch {
            val profile = _profileState.value
            val updatedProfile = profile.copy(pastSeizures = profile.pastSeizures + seizure)
            _profileState.value = updatedProfile
            repository.addSeizure(profile.uid, seizure)
            saveProfile()
        }
    }

    fun removeSeizure(seizure: SeizureEvent) {
        viewModelScope.launch {
            val profile = _profileState.value
            val updatedProfile =
                profile.copy(pastSeizures = profile.pastSeizures.filter { it.timestamp != seizure.timestamp })
            _profileState.value = updatedProfile
            repository.removeSeizure(profile.uid, seizure.timestamp)
            saveProfile()
        }
    }

    fun editSeizure(newSeizure: SeizureEvent, oldSeizure: SeizureEvent) {
        viewModelScope.launch {
            val profile = _profileState.value
            val updatedProfile =
                profile.copy(pastSeizures = profile.pastSeizures.map { if (it.timestamp == oldSeizure.timestamp) newSeizure else it })
            _profileState.value = updatedProfile
            repository.removeSeizure(profile.uid, oldSeizure.timestamp)
            repository.addSeizure(profile.uid, newSeizure)
            saveProfile()
        }
    }

    fun validateDefaultModel(): Metrics {
        val modelPath = "inference_artifacts/inference.onnx"
        val inferenceModelPath =
            copyAssetToInternalStorage(repository.context, modelPath, "defaults.onnx")
        val modelManager = ModelManager(inferenceModelPath)
        return modelManager.validate(repository.context)
    }

    fun registerUser(profile: Profile) {
        val uuid = UUID.randomUUID().toString()
        val newProfile = profile.copy(uid = uuid)
        val metrics = validateDefaultModel()
        val updatedProfile = newProfile.copy(defaultsMetrics = metrics)
        _profileState.value = updatedProfile
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
            repository.updateProfileField(key, value)

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
            saveProfile()
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
            saveProfile()
            Log.d("ProfileViewModel", "Saved training preference: isEnabled=$isEnabled")
        }
    }

    fun updateEmergencyContacts(contact: EmergencyContact, isAdding: Boolean) =
        viewModelScope.launch {
            val updatedContacts = if (isAdding) {
                if (_profileState.value.emergencyContacts.size < 5) _profileState.value.emergencyContacts + contact else _profileState.value.emergencyContacts
            } else {
                _profileState.value.emergencyContacts.filter { it.phone != contact.phone }
            }
            _profileState.update { currentProfile ->
                currentProfile.copy(emergencyContacts = updatedContacts)
            }
            Log.d("ProfileViewModel", "Updated emergencyContacts: $updatedContacts")
            saveProfile()
        }


    @Throws(IOException::class)
    fun copyAssetToInternalStorage(
        context: Context,
        assetPath: String,
        destinationFileName: String
    ): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(assetPath)
        val file = File(context.filesDir, destinationFileName)
        val outputStream = FileOutputStream(file)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _profileState.update { currentProfile ->
                currentProfile.copy(pwd = newPassword)
            }
            repository.updateProfileField("pwd", newPassword)
            saveProfile() // Sauvegarder dans Firestore aussi
            Log.d("ProfileViewModel", "Password updated successfully")
        }
    }

    fun exportSeizures(context: Context) {
        viewModelScope.launch {
            try {
                val seizures = _profileState.value.pastSeizures
                val gson = GsonBuilder().setPrettyPrinting().create()
                pendingJsonExport = gson.toJson(seizures)

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val filename = "seizures_$timestamp.json"

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, filename)
                }

                (context as? Activity)?.startActivityForResult(intent, EXPORT_JSON_REQUEST_CODE)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to export data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun handleExportResult(context: Context, uri: Uri?) {
        viewModelScope.launch {
            if (uri == null || pendingJsonExport == null) {
                Toast.makeText(context, "Export cancelled", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(pendingJsonExport!!.toByteArray())
                }
                Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to write file: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                pendingJsonExport = null
            }
        }
    }

    companion object {
        const val EXPORT_JSON_REQUEST_CODE = 123
    }
}


class ProfileViewModelFactory(
    private val context: Context,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(context, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}