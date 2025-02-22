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
import android.location.Location
import android.net.Uri
import com.epfl.ch.seizureguard.medication_tracker.Medication
import com.epfl.ch.seizureguard.medication_tracker.MedicationLog
import com.google.firebase.messaging.FirebaseMessaging
import com.epfl.ch.seizureguard.widgets.SeizureWidgetUpdater
import com.epfl.ch.seizureguard.widgets.SeizureCountWidget
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime

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

    private val _isInferenceRunning = MutableStateFlow(false)
    val isInferenceRunning: StateFlow<Boolean> = _isInferenceRunning
    fun setInferenceRunning(newValue: Boolean) {
        _isInferenceRunning.value = newValue
    }

    private val _latestLocation = MutableStateFlow<Location?>(null)
    val latestLocation: StateFlow<Location?> = _latestLocation
    fun setLatestLocation(newLocation: Location?) {
        _latestLocation.value = newLocation
    }

    fun requestTraining() {
        val intent = Intent(getApplication(), InferenceService::class.java).apply {
            action = "ACTION_START_TRAINING"
            putExtra("IS_DEBUG_ENABLED", profileState.value.isDebugEnabled)
            putExtra("IS_TRAINING_ENABLED", profileState.value.isTrainingEnabled)
        }

        // Send action to Inference Service
        getApplication<Application>().startService(intent)
    }

    fun updateMetricsFromUI(metrics: Metrics) {
        viewModelScope.launch {
            try {
                repository.updateMetrics(metrics)
                _profileState.update { currentProfile ->
                    currentProfile.copy(latestMetrics = metrics)
                }
                val updatedProfile = _profileState.value

                repository.saveProfileToFirestore(updatedProfile)
                repository.saveProfileToPreferences(updatedProfile)
            } catch (_: Exception) {
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
    }

    fun loadProfileFromEmail(email: String, password: String, onComplete: (Profile?) -> Unit) {
        viewModelScope.launch {
            try {
                val profile = repository.loadProfileFromFirestore(email, password)
                if (profile != null) {
                    _profileState.value = profile
                    repository.saveProfileToPreferences(profile)
                    repository.updateMetrics(profile.latestMetrics)
                    repository.setAuthenticated(true)
                    onComplete(profile)
                } else {
                    Log.d("ProfileViewModel", "Authentication failed for email: $email")
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to load profile from Firestore", e)
                onComplete(null)
            }
        }
    }

    fun logout() = viewModelScope.launch {
        // Reset authentication state
        repository.setAuthenticated(false)
        // Reset all other states
        repository.resetPreferences()
        // Reset profile state
        _profileState.value = Profile.empty()
        // Reset inference state
        _isInferenceRunning.value = false
    }

    fun saveProfile() = viewModelScope.launch {
        try {
            val profile = _profileState.value
            repository.saveProfileToPreferences(profile)
            repository.saveProfileToFirestore(profile)
            SeizureWidgetUpdater.updateWidgets(getApplication())
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error saving profile", e)
        }
    }

    fun setAuthenticated(authenticated: Boolean) = viewModelScope.launch {
        repository.setAuthenticated(authenticated)
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
        }
    }

    fun addSeizure(seizure: SeizureEvent) {
        viewModelScope.launch {
            val currentProfile = _profileState.value
            val updatedSeizures = currentProfile.pastSeizures + seizure
            _profileState.value = currentProfile.copy(pastSeizures = updatedSeizures)
            saveProfile()

            SeizureCountWidget.updateWidget(getApplication())
        }
    }

    fun removeSeizure(seizure: SeizureEvent) {
        viewModelScope.launch {
            val currentProfile = _profileState.value
            val updatedSeizures = currentProfile.pastSeizures.filter { it != seizure }
            _profileState.value = currentProfile.copy(pastSeizures = updatedSeizures)
            saveProfile()

            SeizureCountWidget.updateWidget(getApplication())
        }
    }

    fun editSeizure(newSeizure: SeizureEvent, oldSeizure: SeizureEvent) {
        viewModelScope.launch {
            val profile = _profileState.value
            val updatedProfile = profile.copy(pastSeizures = profile.pastSeizures.map {
                if (it.timestamp == oldSeizure.timestamp) newSeizure else it
            })
            _profileState.value = updatedProfile
            repository.removeSeizure(profile.uid, oldSeizure.timestamp)
            repository.addSeizure(profile.uid, newSeizure)
            saveProfile()
            SeizureWidgetUpdater.updateWidgets(getApplication())
        }
    }

    private fun validateDefaultModel(): Metrics {
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
        }
    }

    fun updateProfileField(key: String, value: String) {
        viewModelScope.launch {
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

            repository.updateProfileField(key, value)
            saveProfile()
        }
    }

    fun updateMultipleFieldsAndSave(updates: Map<String, String>) {
        viewModelScope.launch {
            try {
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

                val updatedProfile = _profileState.value
                repository.saveProfileToPreferences(updatedProfile)
                repository.saveProfileToFirestore(updatedProfile)
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
        }
    }

    fun saveParentPreference(isEnabled: Boolean) {
        viewModelScope.launch {
            repository.saveParentPreference(isEnabled)
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
            saveProfile()
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

    fun retrieveAndStoreFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(
                        "ProfileViewModel",
                        "Fetching FCM registration token failed",
                        task.exception
                    )
                    return@addOnCompleteListener
                }
                // Get the new FCM registration token
                val profile = _profileState.value
                val token = task.result

                // If the token is not null or empty, store it in Firestore
                if (!token.isNullOrEmpty()) {
                    viewModelScope.launch {
                        try {
                            repository.storeFcmToken(profile.uid, token)
                        } catch (e: Exception) {
                            Log.e("ProfileViewModel", "Error storing FCM token", e)
                        }
                    }
                }
            }
    }

    fun sendNotificationToMyDevices(
        title: String,
        location: Location?
    ) {
        val uid = _profileState.value.uid
        if (uid.isNullOrEmpty()) {
            Log.w("ProfileViewModel", "Cannot send notification without a valid UID")
            return
        }

        val latString = location?.latitude?.toString()
        val lonString = location?.longitude?.toString()

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
                repository.sendFcmNotificationToTokens(tokens, title, latString, lonString)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error sending notifications", e)
            }
        }
    }

    fun updateProfileImage(newUri: Uri, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("profile_images")
                    .child(profileState.value.uid + ".jpg")

                val uploadTask = storageRef.putFile(newUri)
                uploadTask.await()

                val downloadUrl = storageRef.downloadUrl.await()

                _profileState.update { currentProfile ->
                    currentProfile.copy(uri = downloadUrl.toString())
                }

                saveProfile()
                onComplete()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile image", e)
                onComplete()
            }
        }
    }

    fun addNote(note: Notes) {
        viewModelScope.launch {
            try {
                val updatedNotes = _profileState.value.medicalNotes.toMutableList()
                updatedNotes.add(note)

                _profileState.value = _profileState.value.copy(
                    medicalNotes = updatedNotes
                )

                repository.saveProfileToPreferences(_profileState.value)
                repository.saveProfileToFirestore(_profileState.value)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error adding note: ${e.message}")
            }
        }
    }

    fun deleteNote(note: Notes) {
        viewModelScope.launch {
            try {
                val updatedNotes = _profileState.value.medicalNotes.filter { it != note }

                _profileState.value = _profileState.value.copy(
                    medicalNotes = updatedNotes
                )

                repository.saveProfileToPreferences(_profileState.value)
                repository.saveProfileToFirestore(_profileState.value)

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting note: ${e.message}")
            }
        }
    }

    fun addMedication(medication: Medication) {
        viewModelScope.launch {
            try {
                _profileState.update { currentProfile ->
                    currentProfile.copy(
                        medications = currentProfile.medications + medication
                    )
                }
                saveProfile()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error adding medication", e)
            }
        }
    }

    fun removeMedication(medicationId: String) {
        viewModelScope.launch {
            try {
                _profileState.update { currentProfile ->
                    currentProfile.copy(
                        medications = currentProfile.medications.filter { it.id != medicationId },
                        medicationLogs = currentProfile.medicationLogs.filter { it.medicationId != medicationId }
                    )
                }
                saveProfile()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error removing medication", e)
            }
        }
    }

    fun logMedication(medicationId: String, timestamp: LocalDateTime = LocalDateTime.now()) {
        _profileState.update { currentState ->
            currentState.copy(
                medicationLogs = currentState.medicationLogs + MedicationLog(
                    medicationId = medicationId,
                    timestamp = timestamp,
                    effectiveness = 5, // Default value
                    mood = 5, // Default value
                    sideEffects = null,
                    notes = null
                )
            )
        }
        saveProfile()
    }

    fun logMedicationWithDetails(logEntry: MedicationLog) {
        _profileState.update { currentState ->
            currentState.copy(
                medicationLogs = currentState.medicationLogs + logEntry
            )
        }
        saveProfile()
    }

    fun getMedicationLogs(medicationId: String): List<MedicationLog> {
        return _profileState.value.medicationLogs.filter { it.medicationId == medicationId }
    }

    fun getMedicationById(medicationId: String): Medication? {
        return _profileState.value.medications.find { it.id == medicationId }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            try {
                _profileState.update { currentProfile ->
                    currentProfile.copy(
                        medications = currentProfile.medications.map { 
                            if (it.id == medication.id) medication else it 
                        }
                    )
                }
                saveProfile()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating medication", e)
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