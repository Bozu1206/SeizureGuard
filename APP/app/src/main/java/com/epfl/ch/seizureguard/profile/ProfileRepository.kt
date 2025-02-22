// ProfileRepository.kt
package com.epfl.ch.seizureguard.profile

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.medication_tracker.LocalDateTimeDeserializer
import com.epfl.ch.seizureguard.medication_tracker.LocalDateTimeSerializer
import com.epfl.ch.seizureguard.medication_tracker.Medication
import com.epfl.ch.seizureguard.medication_tracker.MedicationLog
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import com.epfl.ch.seizureguard.seizure_event.SeizureLocation
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

val Context.dataStore by preferencesDataStore(name = "user_profile")

object Keys {
    val USER_ID = stringPreferencesKey("user_id")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_EMAIL = stringPreferencesKey("user_email")
    val USER_DOB = stringPreferencesKey("user_dob")
    val USER_PP = stringPreferencesKey("user_pp")
    val USER_PWD = stringPreferencesKey("user_pwd")
    val USER_EPI_TYPE = stringPreferencesKey("user_epi_type")
    val EMERGENCY_CONTACTS = stringPreferencesKey("emergency_contacts")
    val AUTH_MODE = stringPreferencesKey("auth_mode")
    val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    val IS_TRAINING_ENABLED = booleanPreferencesKey("is_training_enabled")
    val IS_PARENT_MODE = booleanPreferencesKey("is_parent_mode")
    val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
    val IS_DEBUG_ENABLED = booleanPreferencesKey("debug_mode")
    val POWER_MODE = stringPreferencesKey("power_mode")
    val PAST_SEIZURES = stringPreferencesKey("past_seizures")
    val DEF_METRICS = stringPreferencesKey("def_metrics")
    val LATEST_METRICS = stringPreferencesKey("latest_metrics")
    val LOCAL_MODEL_PATH = stringPreferencesKey("local_model_path")
    val MEDICATIONS = stringPreferencesKey("medications")
    val MEDICAL_NOTES = stringPreferencesKey("medical_notes")
    val MEDICATION_LOGS = stringPreferencesKey("medication_logs")
}

class ProfileRepository private constructor(
    val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
        .create()

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ProfileRepository? = null

        fun getInstance(
            context: Context,
            firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
            storage: FirebaseStorage = FirebaseStorage.getInstance()
        ): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileRepository(
                    context.applicationContext,
                    firestore,
                    storage
                ).also { INSTANCE = it }
            }
        }
    }

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount

    private val _isTrainReady = MutableStateFlow(false)
    val isTrainReady: StateFlow<Boolean> = _isTrainReady

    fun incrementSampleCount() {
        val newCount = _sampleCount.value + 1
        _sampleCount.value = newCount
        if (newCount >= 100) {
            _isTrainReady.value = true
        }
    }

    fun resetSamples() {
        _sampleCount.value = 0
        _isTrainReady.value = false
    }

    private val _latestMetrics = MutableStateFlow(Metrics())
    val latestMetrics: StateFlow<Metrics> = _latestMetrics

    init {
        runBlocking {
            try {
                val preferences = context.dataStore.data.first()
                val latestMetricsJson = preferences[Keys.LATEST_METRICS]
                if (latestMetricsJson != null) {
                    val metrics = gson.fromJson(latestMetricsJson, Metrics::class.java)
                    if (metrics != null) {
                        _latestMetrics.value = metrics
                    } else {
                        Log.w(
                            "ProfileRepository",
                            "Could not parse metrics from JSON: $latestMetricsJson"
                        )
                    }
                } else {
                    Log.d("ProfileRepository", "No stored metrics found in preferences")
                }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Error initializing metrics", e)
            }
        }
    }

    suspend fun updateMetrics(metrics: Metrics) {
        _latestMetrics.value = metrics

        context.dataStore.edit { preferences ->
            preferences[Keys.LATEST_METRICS] = gson.toJson(metrics)
        }

        try {
            val currentProfile = loadProfileFromPreferences()
            val updatedProfile = currentProfile.copy(latestMetrics = metrics)
            saveProfileToFirestore(updatedProfile)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error updating metrics in Firestore", e)
        }
    }

    suspend fun saveProfileToPreferences(profile: Profile) {
        try {
            context.dataStore.edit { preferences ->
                preferences[Keys.USER_ID] = profile.uid
                preferences[Keys.USER_NAME] = profile.name
                preferences[Keys.USER_EMAIL] = profile.email
                preferences[Keys.USER_DOB] = profile.birthdate
                preferences[Keys.USER_PP] = profile.uri
                preferences[Keys.USER_PWD] = profile.pwd
                preferences[Keys.USER_EPI_TYPE] = profile.epi_type
                preferences[Keys.AUTH_MODE] = profile.auth_mode
                preferences[Keys.IS_BIOMETRIC_ENABLED] = profile.isBiometricEnabled
                preferences[Keys.IS_TRAINING_ENABLED] = profile.isTrainingEnabled
                preferences[Keys.EMERGENCY_CONTACTS] = gson.toJson(profile.emergencyContacts)
                preferences[Keys.PAST_SEIZURES] = gson.toJson(profile.pastSeizures)
                preferences[Keys.DEF_METRICS] = gson.toJson(profile.defaultsMetrics)
                preferences[Keys.LATEST_METRICS] = gson.toJson(profile.latestMetrics)
                preferences[Keys.MEDICATIONS] = gson.toJson(profile.medications)
                preferences[Keys.MEDICAL_NOTES] = gson.toJson(profile.medicalNotes)
                preferences[Keys.MEDICATION_LOGS] = gson.toJson(profile.medicationLogs)
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error saving profile to preferences", e)
        }
    }

    suspend fun loadProfileFromPreferences(): Profile {
        val preferences = context.dataStore.data.first()
        val contactsJson = preferences[Keys.EMERGENCY_CONTACTS] ?: "[]"
        val contacts: List<EmergencyContact> = try {
            gson.fromJson(contactsJson, object : TypeToken<List<EmergencyContact>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse emergencyContacts: ${e.message}", e)
            emptyList()
        }

        val pastSeizuresJson = preferences[Keys.PAST_SEIZURES] ?: "[]"
        val pastSeizures: List<SeizureEvent> = try {
            gson.fromJson(pastSeizuresJson, object : TypeToken<List<SeizureEvent>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse pastSeizures: ${e.message}", e)
            emptyList()
        }

        val defMetricsJson = preferences[Keys.DEF_METRICS] ?: "[]"
        val defMetrics: Metrics = try {
            gson.fromJson(defMetricsJson, Metrics::class.java) ?: Metrics()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse defaultMetrics: ${e.message}", e)
            Metrics()
        }

        val latestMetricsJson = preferences[Keys.LATEST_METRICS] ?: "[]"
        val latestMetrics: Metrics = try {
            gson.fromJson(latestMetricsJson, Metrics::class.java) ?: Metrics()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse latestMetrics: ${e.message}", e)
            Metrics()
        }

        val medicationsJson = preferences[Keys.MEDICATIONS] ?: "[]"
        val medications: List<Medication> = try {
            gson.fromJson(medicationsJson, object : TypeToken<List<Medication>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse medications: ${e.message}", e)
            emptyList()
        }

        val medicalNotesJson = preferences[Keys.MEDICAL_NOTES] ?: "[]"
        val medicalNotes: List<Notes> = try {
            gson.fromJson(medicalNotesJson, object : TypeToken<List<Notes>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse medicalNotes: ${e.message}", e)
            emptyList()
        }

        val medicationLogsJson = preferences[Keys.MEDICATION_LOGS] ?: "[]"
        val medicationLogs: List<MedicationLog> = try {
            gson.fromJson(medicationLogsJson, object : TypeToken<List<MedicationLog>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse medicationLogs: ${e.message}", e)
            emptyList()
        }

        return Profile(
            uid = preferences[Keys.USER_ID] ?: "",
            name = preferences[Keys.USER_NAME] ?: "",
            email = preferences[Keys.USER_EMAIL] ?: "",
            birthdate = preferences[Keys.USER_DOB] ?: "",
            uri = preferences[Keys.USER_PP] ?: "",
            pwd = preferences[Keys.USER_PWD] ?: "",
            epi_type = preferences[Keys.USER_EPI_TYPE] ?: "",
            auth_mode = preferences[Keys.AUTH_MODE] ?: "",
            isBiometricEnabled = preferences[Keys.IS_BIOMETRIC_ENABLED] ?: false,
            isTrainingEnabled = preferences[Keys.IS_TRAINING_ENABLED] ?: false,
            isDebugEnabled = preferences[Keys.IS_DEBUG_ENABLED] ?: false,
            powerMode = preferences[Keys.POWER_MODE] ?: "Normal",
            emergencyContacts = contacts,
            pastSeizures = pastSeizures,
            defaultsMetrics = defMetrics,
            latestMetrics = latestMetrics,
            medications = medications,
            medicationLogs = medicationLogs,
            medicalNotes = medicalNotes
        )
    }

    suspend fun saveProfileToFirestore(profile: Profile) {
        try {
            if (profile.uid.isEmpty()) {
                Log.e("ProfileRepository", "Cannot save profile with empty UID")
                return
            }

            Log.d("ProfileRepository", "Saving profile to Firestore with ID: ${profile.uid}")
            Log.d("ProfileRepository", "Current medications count: ${profile.medications.size}")
            Log.d("ProfileRepository", "Current medication logs count: ${profile.medicationLogs.size}")

            val imageUri = Uri.parse(profile.uri)
            if (imageUri.scheme.equals("content", ignoreCase = true)
                || imageUri.scheme.equals("file", ignoreCase = true)
            ) {
                val imageRef = storage.reference.child("profile_images/${profile.uid}.jpg")
                imageRef.putFile(imageUri).await()

                val downloadUrl = imageRef.downloadUrl.await()
                profile.uri = downloadUrl.toString()
            }

            // Convert medication logs to a format that Firestore can store
            val medicationLogsData = profile.medicationLogs.map { log ->
                mapOf(
                    "medicationId" to log.medicationId,
                    "timestamp" to log.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "effectiveness" to log.effectiveness,
                    "mood" to log.mood,
                    "sideEffects" to log.sideEffects,
                    "notes" to log.notes
                )
            }

            // Create a map of the profile data
            val profileData = mapOf(
                "name" to profile.name,
                "email" to profile.email,
                "birthdate" to profile.birthdate,
                "uri" to profile.uri,
                "pwd" to profile.pwd,
                "epi_type" to profile.epi_type,
                "auth_mode" to profile.auth_mode,
                "isBiometricEnabled" to profile.isBiometricEnabled,
                "isTrainingEnabled" to profile.isTrainingEnabled,
                "isDebugEnabled" to profile.isDebugEnabled,
                "powerMode" to profile.powerMode,
                "emergencyContacts" to profile.emergencyContacts,
                "pastSeizures" to profile.pastSeizures.map { seizure ->
                    mapOf(
                        "timestamp" to seizure.timestamp,
                        "type" to seizure.type,
                        "duration" to seizure.duration,
                        "severity" to seizure.severity,
                        "latitude" to seizure.location.latitude,
                        "longitude" to seizure.location.longitude,
                        "triggers" to seizure.triggers
                    )
                },
                "defaultsMetrics" to profile.defaultsMetrics,
                "latestMetrics" to profile.latestMetrics,
                "medications" to profile.medications.map { medication ->
                    mapOf(
                        "id" to medication.id,
                        "name" to medication.name,
                        "dosage" to medication.dosage,
                        "frequency" to medication.frequency,
                        "shape" to medication.shape,
                        "timeOfDay" to medication.timeOfDay.map { time ->
                            // Store as ISO-8601 string format
                            time.toString()
                        }
                    )
                },
                "medicationLogs" to medicationLogsData,
                "medicalNotes" to profile.medicalNotes
            )

            // Use the profile's uid as the document ID
            firestore.collection("profiles")
                .document(profile.uid)
                .set(profileData)
                .await()

            Log.d("ProfileRepository", "Successfully saved profile to Firestore")
            Log.d("ProfileRepository", "Saved medications count: ${profile.medications.size}")
            Log.d("ProfileRepository", "Saved medication logs count: ${medicationLogsData.size}")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error saving profile to Firestore", e)
            e.printStackTrace()
            throw e
        }
    }

    private inline fun <reified T> getFirestoreValue(value: Any?, defaultValue: T): T {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is T -> value
            is Map<*, *> -> {
                when (defaultValue) {
                    is String -> value["stringValue"] as? T ?: defaultValue
                    is Boolean -> value["booleanValue"] as? T ?: defaultValue
                    is Int -> (value["integerValue"] as? String)?.toInt() as? T ?: defaultValue
                    is Double -> (value["doubleValue"] as? String)?.toDouble() as? T ?: defaultValue
                    is Long -> (value["longValue"] as? String)?.toLong() as? T ?: defaultValue
                    else -> defaultValue
                }
            }
            else -> defaultValue
        }
    }

    private fun parseSeizureEvent(data: Map<String, Any>): SeizureEvent {
        val location = try {
            val latitude = getFirestoreValue(data["latitude"], null as Double?)
            val longitude = getFirestoreValue(data["longitude"], null as Double?)
            if (latitude != null && longitude != null) {
                SeizureLocation(latitude, longitude)
            } else {
                SeizureLocation()
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error parsing location: ${e.message}")
            SeizureLocation()
        }

        val triggers = try {
            @Suppress("UNCHECKED_CAST")
            (data["triggers"] as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error parsing triggers: ${e.message}")
            emptyList()
        }

        return SeizureEvent(
            timestamp = getFirestoreValue(data["timestamp"], 0L),
            type = getFirestoreValue(data["type"], ""),
            duration = getFirestoreValue(data["duration"], 0),
            severity = getFirestoreValue(data["severity"], 1),
            location = location,
            triggers = triggers
        )
    }

    private fun parseMedicationLog(data: Map<String, Any>): MedicationLog {
        val timestamp = try {
            val timestampStr = getFirestoreValue(data["timestamp"], "")
            if (timestampStr.isNotEmpty()) {
                try {
                    LocalDateTime.parse(timestampStr)
                } catch (e: Exception) {
                    Log.e("ProfileRepository", "Error parsing timestamp string: $timestampStr", e)
                    LocalDateTime.now()
                }
            } else {
                LocalDateTime.now()
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error getting timestamp value", e)
            LocalDateTime.now()
        }

        return MedicationLog(
            medicationId = getFirestoreValue(data["medicationId"], ""),
            timestamp = timestamp,
            effectiveness = getFirestoreValue(data["effectiveness"], 5),
            mood = getFirestoreValue(data["mood"], 5),
            sideEffects = getFirestoreValue(data["sideEffects"], null as String?),
            notes = getFirestoreValue(data["notes"], null as String?)
        )
    }

    suspend fun loadProfileFromFirestore(email: String, password: String): Profile? {
        try {
            Log.d("ProfileRepository", "Attempting to load profile for email: $email")
            
            val document = firestore.collection("profiles")
                .whereEqualTo("email", email)
                .get()
                .await()
                .documents
                .firstOrNull() ?: return null
            
            val data = document.data ?: return null
            val userId = document.id
            
            // Verify password
            val storedPassword = getFirestoreValue(data["pwd"], "")
            if (storedPassword != password) {
                Log.d("ProfileRepository", "Password mismatch for email: $email")
                return null
            }
            
            // Parse medication logs with better error handling
            val medicationLogs = try {
                (data["medicationLogs"] as? List<Map<String, Any>>)?.mapNotNull { logData -> 
                    try {
                        parseMedicationLog(logData)
                    } catch (e: Exception) {
                        Log.e("ProfileRepository", "Error parsing individual medication log: ${e.message}")
                        null
                    }
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Error parsing medication logs array: ${e.message}")
                emptyList()
            }
            
            Log.d("ProfileRepository", "Successfully parsed ${medicationLogs.size} medication logs")
            
            // Parse past seizures
            val pastSeizures = (data["pastSeizures"] as? List<Map<String, Any>>)?.map { 
                parseSeizureEvent(it)
            } ?: emptyList()

            // Parse medications with better error handling
            val medications = try {
                (data["medications"] as? List<Map<String, Any>>)?.mapNotNull { medicationData ->
                    try {
                        val timeOfDayList = (medicationData["timeOfDay"] as? List<*>)?.mapNotNull { timeStr ->
                            try {
                                when (timeStr) {
                                    is String -> LocalDateTime.parse(timeStr)
                                    is Map<*, *> -> LocalDateTime.parse(timeStr["stringValue"] as? String ?: return@mapNotNull null)
                                    else -> null
                                }
                            } catch (e: Exception) {
                                Log.e("ProfileRepository", "Error parsing individual timeOfDay: ${e.message}")
                                null
                            }
                        } ?: emptyList()

                        Medication(
                            id = getFirestoreValue(medicationData["id"], UUID.randomUUID().toString()),
                            name = getFirestoreValue(medicationData["name"], ""),
                            dosage = getFirestoreValue(medicationData["dosage"], ""),
                            frequency = getFirestoreValue(medicationData["frequency"], ""),
                            shape = getFirestoreValue(medicationData["shape"], ""),
                            timeOfDay = timeOfDayList
                        )
                    } catch (e: Exception) {
                        Log.e("ProfileRepository", "Error parsing individual medication: ${e.message}")
                        null
                    }
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Error parsing medications array: ${e.message}")
                emptyList()
            }

            Log.d("ProfileRepository", "Successfully parsed ${medications.size} medications with their schedules")
            
            val profile = Profile(
                uid = userId,
                name = getFirestoreValue(data["name"], ""),
                email = getFirestoreValue(data["email"], ""),
                birthdate = getFirestoreValue(data["birthdate"], ""),
                uri = getFirestoreValue(data["uri"], ""),
                pwd = storedPassword,
                epi_type = getFirestoreValue(data["epi_type"], ""),
                auth_mode = getFirestoreValue(data["auth_mode"], ""),
                isBiometricEnabled = getFirestoreValue(data["isBiometricEnabled"], false),
                isTrainingEnabled = getFirestoreValue(data["isTrainingEnabled"], false),
                isDebugEnabled = getFirestoreValue(data["isDebugEnabled"], false),
                powerMode = getFirestoreValue(data["powerMode"], "Normal"),
                emergencyContacts = (data["emergencyContacts"] as? List<Map<String, Any>>)?.map { 
                    EmergencyContact(
                        name = getFirestoreValue(it["name"], ""),
                        phone = getFirestoreValue(it["phone"], ""),
                        photoUri = getFirestoreValue(it["photoUri"], null as String?)
                    )
                } ?: emptyList(),
                pastSeizures = pastSeizures,
                defaultsMetrics = Metrics(), // TODO: Add proper Metrics deserialization
                latestMetrics = Metrics(), // TODO: Add proper Metrics deserialization
                medications = medications,
                medicationLogs = medicationLogs,
                medicalNotes = (data["medicalNotes"] as? List<Map<String, Any>>)?.map {
                    Notes(
                        title = getFirestoreValue(it["title"], ""),
                        content = getFirestoreValue(it["content"], "")
                    )
                } ?: emptyList()
            )

            // Load profile picture if available
            profile.uri = loadProfilePicture(userId)?.toString() ?: ""
            
            // Update local metrics state
            _latestMetrics.value = profile.latestMetrics

            Log.d("ProfileRepository", "Profile loaded successfully: ${profile.medications.size} medications, ${profile.medicationLogs.size} logs")
            return profile
            
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to load profile from Firestore", e)
            return null
        }
    }

    private suspend fun loadProfilePicture(uid: String): Uri? {
        return try {
            val imageRef = storage.reference.child("profile_images/$uid.jpg")
            val uri = imageRef.downloadUrl.await()
            uri
        } catch (e: Exception) {
            Log.e(
                "ProfileRepository",
                "Failed to load profile picture from Firebase Storage: ${e.message}",
                e
            )
            null
        }
    }

    suspend fun addSeizure(userId: String, seizure: SeizureEvent) {
        try {
            // Get the current document data
            val documentSnapshot = firestore.collection("profiles")
                .document(userId)
                .get()
                .await()
            
            val data = documentSnapshot.data ?: return
            
            // Parse existing seizures using our custom parser
            val existingSeizures = (data["pastSeizures"] as? List<Map<String, Any>>)?.map { 
                parseSeizureEvent(it)
            } ?: emptyList()
            
            val updatedSeizures = existingSeizures + seizure
            
            // Update only the pastSeizures field
            firestore.collection("profiles")
                .document(userId)
                .update("pastSeizures", updatedSeizures.map { event ->
                    mapOf(
                        "timestamp" to event.timestamp,
                        "type" to event.type,
                        "duration" to event.duration,
                        "severity" to event.severity,
                        "latitude" to event.location.latitude,
                        "longitude" to event.location.longitude,
                        "triggers" to event.triggers
                    )
                })
                .await()
            
            Log.d("ProfileRepository", "Successfully added seizure event with location: ${seizure.location.latitude}, ${seizure.location.longitude}")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to add seizure event", e)
            throw e
        }
    }

    suspend fun removeSeizure(userId: String, timestamp: Long) {
        try {
            // Get the current document data
            val documentSnapshot = firestore.collection("profiles")
                .document(userId)
                .get()
                .await()
            
            val data = documentSnapshot.data ?: return
            
            // Parse existing seizures using our custom parser
            val existingSeizures = (data["pastSeizures"] as? List<Map<String, Any>>)?.map { 
                parseSeizureEvent(it)
            } ?: emptyList()
            
            // Filter out the seizure to remove
            val updatedSeizures = existingSeizures.filter { it.timestamp != timestamp }
            
            // Update the pastSeizures field
            firestore.collection("profiles")
                .document(userId)
                .update("pastSeizures", updatedSeizures.map { event ->
                    mapOf(
                        "timestamp" to event.timestamp,
                        "type" to event.type,
                        "duration" to event.duration,
                        "severity" to event.severity,
                        "latitude" to event.location.latitude,
                        "longitude" to event.location.longitude,
                        "triggers" to event.triggers
                    )
                })
                .await()
            
            Log.d("ProfileRepository", "Successfully removed seizure event with timestamp: $timestamp")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to remove seizure event", e)
            throw e
        }
    }

    suspend fun saveAuthPreference(isBiometric: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTH_MODE] = if (isBiometric) "biometric" else "password"
            preferences[Keys.IS_BIOMETRIC_ENABLED] = isBiometric
        }
    }

    suspend fun resetPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun updateProfileField(key: String, value: String) {
        try {
            val currentProfile = loadProfileFromPreferences()

            val updatedProfile = when (key) {
                "name" -> currentProfile.copy(name = value)
                "email" -> currentProfile.copy(email = value)
                "birthdate" -> currentProfile.copy(birthdate = value)
                "uri" -> currentProfile.copy(uri = value)
                "pwd" -> currentProfile.copy(pwd = value)
                "epi_type" -> currentProfile.copy(epi_type = value)
                "auth_mode" -> currentProfile.copy(auth_mode = value)
                else -> currentProfile
            }

            saveProfileToPreferences(updatedProfile)
            saveProfileToFirestore(updatedProfile)

        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error updating profile field", e)
            throw e
        }
    }

    suspend fun setAuthenticated(authenticated: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_AUTHENTICATED] = authenticated
        }
    }

    suspend fun saveTrainingPreference(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_TRAINING_ENABLED] = isEnabled
        }
    }

    suspend fun saveDebugPreference(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_DEBUG_ENABLED] = isEnabled
        }
    }

    suspend fun savePowerModePreference(powerMode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.POWER_MODE] = powerMode
        }
    }

    suspend fun saveParentPreference(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_PARENT_MODE] = isEnabled
        }
    }

    fun getParentPreference(): Flow<Boolean> {
        return context.dataStore.data
            .map { preferences -> preferences[Keys.IS_PARENT_MODE] ?: false }
    }

    fun saveModelToFirebase(modelFile: File) {
        runBlocking {
            try {
                val preferences = context.dataStore.data.first()
                val userId = preferences[Keys.USER_ID] ?: return@runBlocking

                val localModelFile = File(context.filesDir, "local_model.onnx")
                modelFile.copyTo(localModelFile, overwrite = true)

                context.dataStore.edit { prefs ->
                    prefs[Keys.LOCAL_MODEL_PATH] = localModelFile.absolutePath
                }

                val modelRef = storage.reference
                    .child("models")
                    .child("$userId.onnx")

                modelRef.putFile(Uri.fromFile(modelFile))
                    .addOnSuccessListener {
                        Log.d("ProfileRepository", "Model uploaded successfully for user $userId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileRepository", "Error uploading model for user $userId", e)
                    }

            } catch (e: Exception) {
                Log.e("ProfileRepository", "Error saving model", e)
            }
        }
    }

    fun loadLatestModelFromFirebase(onComplete: (File?) -> Unit) {
        runBlocking {
            try {
                val preferences = context.dataStore.data.first()
                val userId = preferences[Keys.USER_ID] ?: return@runBlocking onComplete(null)

                val localModelPath = preferences[Keys.LOCAL_MODEL_PATH]
                if (localModelPath != null) {
                    val localFile = File(localModelPath)
                    if (localFile.exists()) {
                        Log.d("ProfileRepository", "Using cached local model: $localModelPath")
                        return@runBlocking onComplete(localFile)
                    }
                }

                val modelRef = storage.reference
                    .child("models")
                    .child("$userId.onnx")

                val downloadedFile = File(context.filesDir, "downloaded_model.onnx")

                modelRef.getFile(downloadedFile)
                    .addOnSuccessListener {
                        val localModelFile = File(context.filesDir, "local_model.onnx")
                        downloadedFile.copyTo(localModelFile, overwrite = true)

                        runBlocking {
                            context.dataStore.edit { prefs ->
                                prefs[Keys.LOCAL_MODEL_PATH] = localModelFile.absolutePath
                            }
                        }
                        onComplete(localModelFile)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileRepository", "Error downloading model", e)
                        onComplete(null)
                    }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Error loading model", e)
                onComplete(null)
            }
        }
    }

    suspend fun storeFcmToken(uid: String, token: String) {
        // Reference to the profile document and its "tokens" sub-collection
        val tokensRef = Firebase.firestore
            .collection("profiles")
            .document(uid)
            .collection("tokens")
        val tokenDocRef = tokensRef.document(token)

        try {
            // Check if this token document already exists
            val documentSnapshot = tokenDocRef.get().await()
            if (!documentSnapshot.exists()) {
                // If it doesn't exist, create/set the token document
                tokenDocRef.set(mapOf("token" to token)).await()
            } else {
                Log.d("ProfileRepository", "Token already exists in 'profiles/$uid/tokens': $token")
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to store FCM token in profile $uid", e)
            throw e
        }
    }

    suspend fun getAllFcmTokens(uid: String): List<String> {
        val tokensRef = Firebase.firestore
            .collection("profiles")
            .document(uid)
            .collection("tokens")

        return try {
            val querySnapshot = tokensRef.get().await()
            querySnapshot.documents.mapNotNull { it.getString("token") }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to retrieve FCM tokens in profile $uid", e)
            emptyList()
        }
    }

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val credentialsStream =
            context.assets.open("seizureguard-1e3d9-firebase-adminsdk-bmgtm-812b9e8cb0.json")
        val googleCredentials = GoogleCredentials.fromStream(credentialsStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        googleCredentials.refreshIfExpired()
        return@withContext googleCredentials.accessToken.tokenValue
    }

    suspend fun sendFcmNotificationToTokens(
        tokens: List<String>,
        title: String,
        latitude: String?,
        longitude: String?
    ) {
        withContext(Dispatchers.IO) {
            val url = "https://fcm.googleapis.com/v1/projects/seizureguard-1e3d9/messages:send"
            val accessToken = getAccessToken()
            tokens.forEach { token ->
                val dataPayload = mutableMapOf(
                    "title" to title
                )
                latitude?.let { dataPayload["latitude"] = it }
                longitude?.let { dataPayload["longitude"] = it }
                val payload = mapOf(
                    "message" to mapOf(
                        "token" to token,
                        "data" to dataPayload,
                        "android" to mapOf("priority" to "high")
                    )
                )
                val jsonPayload = Gson().toJson(payload)
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .post(RequestBody.create("application/json".toMediaType(), jsonPayload))
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Failed to send FCM notification: ${response.body?.string()}")
                }
            }
        }
    }

    suspend fun updateMedications(medications: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MEDICATIONS] = gson.toJson(medications)
        }
    }
}
