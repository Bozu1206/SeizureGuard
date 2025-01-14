// ProfileRepository.kt
package com.epfl.ch.seizureguard.profile

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.FileInputStream

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
}

class ProfileRepository private constructor(
    val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val gson = Gson()

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

    private val _debugMode = MutableStateFlow(false)
    val debugMode: StateFlow<Boolean> = _debugMode

    private val _powerMode = MutableStateFlow("Normal")
    val powerMode: StateFlow<String> = _powerMode

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
                        Log.d("ProfileRepository", "Initialized latestMetrics from preferences: $metrics")
                    } else {
                        Log.w("ProfileRepository", "Could not parse metrics from JSON: $latestMetricsJson")
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
            Log.d("ProfileRepository", "Metrics updated everywhere: $metrics")
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
            }
            Log.d("ProfileRepository", "Profile saved to preferences successfully: $profile")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error saving profile to preferences", e)
        }
    }

    suspend fun loadProfileFromPreferences(): Profile {
        val preferences = context.dataStore.data.first()
        val contactsJson = preferences[Keys.EMERGENCY_CONTACTS] ?: "[]"
        val contacts: List<EmergencyContact> = try {
            gson.fromJson(contactsJson, object : TypeToken<List<EmergencyContact>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse emergencyContacts: ${e.message}", e)
            emptyList()
        }

        val pastSeizuresJson = preferences[Keys.PAST_SEIZURES] ?: "[]"
        val pastSeizures: List<SeizureEvent> = try {
            gson.fromJson(pastSeizuresJson, object : TypeToken<List<SeizureEvent>>() {}.type) ?: emptyList()
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
        val medications: List<String> = try {
            gson.fromJson(medicationsJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to parse medications: ${e.message}", e)
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
            medications = medications
        )
    }

    suspend fun saveProfileToFirestore(profile: Profile) {
        try {
            val userId = context.dataStore.data.first()[Keys.USER_ID] ?: run {
                Log.e("ProfileRepository", "No user ID found in preferences")
                return
            }
            
            Log.d("ProfileRepository", "Starting Firestore save for user $userId")
            Log.d("ProfileRepository", "Profile to save: $profile")

            if (profile.uri.isNotEmpty()) {
                uploadImageToStorage(userId, Uri.parse(profile.uri))
            }

            firestore.collection("profiles")
                .document(userId)
                .set(profile)
                .await()
            
            Log.d("ProfileRepository", "Profile successfully saved to Firestore")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error saving profile to Firestore", e)
            throw e
        }
    }

    private suspend fun uploadImageToStorage(uid: String, imageUri: Uri) {
        try {
            Log.d("ProfileRepository", uid)
            Log.d("ProfileRepository", imageUri.toString())
            val imageRef = storage.reference.child("profile_images/$uid.jpg")
            imageRef.putFile(imageUri).await()
            Log.d("ProfileRepository", "Image uploaded to Firebase Storage: $imageRef")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to upload image to Firebase Storage: ${e.message}", e)
        }
    }

    suspend fun loadProfileFromFirestore(email: String, password: String): Profile? {
        try {
            Log.d("ProfileRepository", "Loading profile from Firestore with email: $email")
            val querySnapshot = firestore.collection("profiles")
                .whereEqualTo("email", email)
                .whereEqualTo("pwd", password)
                .get()
                .await()
            val profile = querySnapshot.documents.mapNotNull { it.toObject<Profile>() }.firstOrNull()
            Log.d("ProfileRepository", "Loaded profile from Firestore: $profile")
            profile?.uri = profile?.uid?.let { loadProfilePicture(it).toString() }.toString()
            return profile
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to load profile from Firestore: ${e.message}", e)
            return null
        }
    }

    private suspend fun loadProfilePicture(uid: String): Uri? {
        try {
            val imageRef = storage.reference.child("profile_images/$uid.jpg")
            val uri = imageRef.downloadUrl.await()
            Log.d("ProfileRepository", "Loaded profile picture from Firebase Storage: $uri")
            return uri
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to load profile picture from Firebase Storage: ${e.message}", e)
            return null
        }
    }

    suspend fun addSeizure(userId: String, seizure: SeizureEvent) {
        val profile = firestore.collection("profiles").document(userId).get().await().toObject<Profile>()
        profile?.pastSeizures = profile?.pastSeizures?.plus(seizure) ?: listOf(seizure)
        firestore.collection("profiles").document(userId).set(profile!!)
    }

    suspend fun removeSeizure(userId: String, timestamp: Long) {
        val profile = firestore.collection("profiles").document(userId).get().await().toObject<Profile>()
        profile?.pastSeizures = profile?.pastSeizures?.filter { it.timestamp != timestamp } ?: emptyList()
        firestore.collection("profiles").document(userId).set(profile!!)
    }

    suspend fun saveAuthPreference(isBiometric: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTH_MODE] = if (isBiometric) "biometric" else "password"
            preferences[Keys.IS_BIOMETRIC_ENABLED] = isBiometric
        }
        Log.d("ProfileRepository", "Saved auth preference: isBiometric=$isBiometric")
    }

    suspend fun resetPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        Log.d("ProfileRepository", "Preferences reset.")
    }

    suspend fun updateProfileField(key: String, value: String) {
        try {
            // Charger le profil actuel
            val currentProfile = loadProfileFromPreferences()
            
            // Mettre à jour le champ spécifique
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

            // Sauvegarder le profil complet dans les préférences
            saveProfileToPreferences(updatedProfile)
            
            // Sauvegarder dans Firestore aussi
            saveProfileToFirestore(updatedProfile)

            Log.d("ProfileRepository", "Updated field $key with value $value")
            Log.d("ProfileRepository", "Updated profile: $updatedProfile")
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
        Log.d("ProfileRepository", "Saved training preference: isEnabled=$isEnabled")
    }

    suspend fun saveDebugPreference(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_DEBUG_ENABLED] = isEnabled
        }
        Log.d("ProfileRepository", "Saved debug preference: isEnabled=$isEnabled")
    }

    suspend fun savePowerModePreference(powerMode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.POWER_MODE] = powerMode
        }
        Log.d("ProfileRepository", "Saved power mode preference: isEnabled=$powerMode")
    }

    suspend fun saveParentPreference(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_PARENT_MODE] = isEnabled
        }
        Log.d("ProfileRepository", "Parent preference saved locally: $isEnabled")
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

                Log.d("ProfileRepository", "Model saved locally at: ${localModelFile.absolutePath}")
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
                        
                        Log.d("ProfileRepository", "Model downloaded and cached locally")
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
                // If it doesn’t exist, create/set the token document
                tokenDocRef.set(mapOf("token" to token)).await()
                Log.d("ProfileRepository", "FCM token stored in 'profiles/$uid/tokens': $token")
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

    suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val credentialsStream = context.assets.open("seizureguard-1e3d9-firebase-adminsdk-bmgtm-812b9e8cb0.json")
        val googleCredentials = GoogleCredentials.fromStream(credentialsStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        googleCredentials.refreshIfExpired()
        return@withContext googleCredentials.accessToken.tokenValue
    }

    suspend fun sendFcmNotificationToTokens(tokens: List<String>, title: String, body: String) {
        withContext(Dispatchers.IO) {
            val url = "https://fcm.googleapis.com/v1/projects/seizureguard-1e3d9/messages:send"
            val accessToken = getAccessToken()
            tokens.forEach { token ->
                val payload = mapOf(
                    "message" to mapOf(
                        "token" to token,
                        "data" to mapOf(
                            "title" to title,
                            "body" to body
                        ),
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
