// ProfileViewModel.kt
package com.epfl.ch.seizureguard.profile

import android.Manifest
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
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.messaging.FirebaseMessaging

class ProfileViewModel(context: Context, application: Application) : AndroidViewModel(application) {
    private val repository: ProfileRepository by lazy {
        ProfileRepository.getInstance(
            context = context
        )
    }

    val parentMode: StateFlow<Boolean> = repository.getParentPreference()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val sampleCount: StateFlow<Int> = repository.sampleCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val isTrainReady: StateFlow<Boolean> = repository.isTrainReady
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val latestMetrics: StateFlow<Metrics> = repository.latestMetrics
        .stateIn(viewModelScope, SharingStarted.Eagerly, Metrics())

    private val _tokensList = MutableStateFlow<List<String>>(emptyList())
    val tokensList: StateFlow<List<String>> = _tokensList

    fun requestTraining() {
        val intent = Intent(getApplication(), InferenceService::class.java).apply {
            action = "ACTION_START_TRAINING"
        }

        getApplication<Application>().startService(intent)
    }

    fun updateMetricsFromUI(metrics: Metrics) {
        viewModelScope.launch {
            try {
                // 1. Mettre à jour les métriques dans le repository
                repository.updateMetrics(metrics)

                // 2. Mettre à jour le state local avec les nouvelles métriques
                _profileState.update { currentProfile ->
                    currentProfile.copy(latestMetrics = metrics)
                }

                // 3. Attendre que le state soit mis à jour
                val updatedProfile = _profileState.value

                // 4. Vérifier que les métriques sont bien mises à jour
                Log.d("ProfileViewModel", "Updating profile with metrics: ${updatedProfile.latestMetrics}")

                // 5. Sauvegarder dans Firebase et les préférences
                repository.saveProfileToFirestore(updatedProfile)
                repository.saveProfileToPreferences(updatedProfile)

                Log.d("ProfileViewModel", "Metrics updated and saved successfully: $metrics")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating metrics", e)
            }
        }
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
        retrieveAndStoreFcmToken()
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
        try {
            val profile = _profileState.value
            Log.d("ProfileViewModel", "Starting to save profile: $profile")
            
            // Sauvegarder dans les préférences
            repository.saveProfileToPreferences(profile)
            Log.d("ProfileViewModel", "Profile saved to preferences")
            
            // Sauvegarder dans Firebase
            repository.saveProfileToFirestore(profile)
            Log.d("ProfileViewModel", "Profile saved to Firestore")
            
            Log.d("ProfileViewModel", "Profile saved successfully with all fields.")
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error saving profile", e)
        }
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

    fun saveDebugPreference(isDebug: Boolean) {
        viewModelScope.launch {
            repository.saveDebugPreference(isDebug)
            _profileState.update { currentState ->
                currentState.copy(
                    isDebugEnabled = isDebug
                )
            }
            saveProfile()
            Log.d("ProfileViewModel", "Saved debug preference: isBiometric=$isDebug")
        }
    }

    fun savePowerModePreference(powerMode: String) {
        viewModelScope.launch {
            repository.savePowerModePreference(powerMode)
            _profileState.update { currentState ->
                currentState.copy(
                    powerMode = powerMode
                )
            }
            saveProfile()
            Log.d("ProfileViewModel", "Saved power model preference: isBiometric=$powerMode")
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
        val profile = _profileState.value
        val modelManager = ModelManager(inferenceModelPath, profile.isDebugEnabled)
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
            // Mettre à jour le state
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

            // Sauvegarder dans les préférences
            repository.updateProfileField(key, value)
            
            // Sauvegarder le profil complet
            saveProfile()
            
            Log.d("ProfileViewModel", "Profile field updated and saved: $key = $value")
        }
    }

    fun updateMultipleFieldsAndSave(updates: Map<String, String>) {
        viewModelScope.launch {
            try {
                // Mettre à jour le state avec toutes les modifications
                _profileState.update { currentProfile ->
                    var updatedProfile = currentProfile
                    updates.forEach { (key, value) ->
                        updatedProfile = when (key) {
                            "name" -> updatedProfile.copy(name = value)
                            "email" -> updatedProfile.copy(email = value)
                            "epi_type" -> updatedProfile.copy(epi_type = value)
                            else -> updatedProfile
                        }
                    }
                    updatedProfile
                }

                Log.d("ProfileViewModel", "State updated with new values: $updates")
                
                // Attendre que le state soit mis à jour
                val updatedProfile = _profileState.value
                Log.d("ProfileViewModel", "Current profile state: $updatedProfile")

                // Sauvegarder explicitement dans les deux endroits
                repository.saveProfileToPreferences(updatedProfile)
                repository.saveProfileToFirestore(updatedProfile)
                
                Log.d("ProfileViewModel", "Multiple fields updated and saved successfully")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating multiple fields", e)
            }
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

    fun saveParentPreference(isEnabled: Boolean) {
        viewModelScope.launch {
            repository.saveParentPreference(isEnabled)
            Log.d("ProfileViewModel", "Parent mode saved: $isEnabled")
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

    fun saveModelToFirebase(modelFile: File) {
        viewModelScope.launch {
            repository.saveModelToFirebase(modelFile)
        }
    }

    fun loadLatestModelFromFirebase(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            repository.loadLatestModelFromFirebase(onComplete)
        }
    }

    fun updateMedications(medications: List<String>) {
        viewModelScope.launch {
            try {
                _profileState.update { currentProfile ->
                    currentProfile.copy(medications = medications)
                }
                repository.updateMedications(medications)
                saveProfile()
                Log.d("ProfileViewModel", "Medications updated successfully: $medications")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating medications", e)
            }
        }
    }

    fun addMedication(medication: String) {
        viewModelScope.launch {
            val currentMedications = _profileState.value.medications
            updateMedications(currentMedications + medication)
        }
    }

    fun removeMedication(medication: String) {
        viewModelScope.launch {
            val currentMedications = _profileState.value.medications
            updateMedications(currentMedications - medication)
        }
    }

    fun retrieveAndStoreFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("ProfileViewModel", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                // Get the new FCM registration token
                val profile = _profileState.value
                val token = task.result
                Log.d("ProfileViewModel", "FCM Token retrieved: $token")
                // If the token is not null or empty, store it in Firestore
                if (!token.isNullOrEmpty()) {
                    viewModelScope.launch {
                        try {
                            repository.storeFcmToken(profile.uid, token)
                            Log.d("ProfileViewModel", "Token successfully stored if not present")
                        } catch (e: Exception) {
                            Log.e("ProfileViewModel", "Error storing FCM token", e)
                        }
                    }
                }
            }
    }

    fun fetchAllFcmTokens() {
        viewModelScope.launch {
            try {
                val profile = _profileState.value
                val tokens = repository.getAllFcmTokens(profile.uid)
                _tokensList.value = tokens
                Log.d("ProfileViewModel", "Successfully retrieved tokens: $tokens")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching all FCM tokens", e)
            }
        }
    }

    fun sendNotificationToMyDevices(
        title: String,
        body: String,
        location: Location?
    ) {
        val uid = _profileState.value.uid
        if (uid.isNullOrEmpty()) {
            Log.w("ProfileViewModel", "Cannot send notification without a valid UID")
            return
        }

        val locationInfo = location?.let {
            "Lat: ${it.latitude}, Long: ${it.longitude}"
        } ?: "Location unavailable"

        val updatedBody = "$body\nLocation: $locationInfo"

        viewModelScope.launch {
            try {
                val tokens = repository.getAllFcmTokens(uid)
                if (tokens.isEmpty()) {
                    Log.w(
                        "ProfileViewModel",
                        "No tokens found for user $uid, skipping notification"
                    )
                    return@launch
                }
                repository.sendFcmNotificationToTokens(tokens, title, updatedBody)
                Log.d("ProfileViewModel", "Notification sent to ${tokens.size} device(s).")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error sending notifications", e)
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