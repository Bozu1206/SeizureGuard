// ProfileRepository.kt
package com.epfl.ch.seizureguard.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.epfl.ch.seizureguard.seizure_event.SeizureEntity
import com.epfl.ch.seizureguard.seizure_event.SeizureEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

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
    val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
    val PAST_SEIZURES = stringPreferencesKey("past_seizures")
}

class ProfileRepository(
    val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {
    private val gson = Gson()

    suspend fun saveProfileToPreferences(profile: Profile) {
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
        }
        Log.d("ProfileRepository", "Profile saved to preferences: $profile")
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
            emergencyContacts = contacts,
            pastSeizures = pastSeizures
        )
    }

    suspend fun saveProfileToFirestore(profile: Profile) {
        if (profile.uid.isEmpty()) {
            Log.e("ProfileRepository", "Profile UID is empty, skipping Firestore save.")
            return
        }

        try {
            val imageUri = Uri.parse(profile.uri)
            uploadImageToStorage(profile.uid, imageUri)

            Log.d("ProfileRepository", "Saving profile to Firestore: $profile")
            firestore.collection("profiles")
                .document(profile.uid)
                .set(profile)
                .await()
            Log.d("ProfileRepository", "Profile successfully saved to Firestore.")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to save profile to Firestore: ${e.message}", e)
        }
    }

    private suspend fun uploadImageToStorage(uid: String, imageUri: Uri) {
        try {
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
        context.dataStore.edit { preferences ->
            when (key) {
                "name" -> preferences[Keys.USER_NAME] = value
                "email" -> preferences[Keys.USER_EMAIL] = value
                "birthdate" -> preferences[Keys.USER_DOB] = value
                "uri" -> preferences[Keys.USER_PP] = value
                "pwd" -> preferences[Keys.USER_PWD] = value
                "epi_type" -> preferences[Keys.USER_EPI_TYPE] = value
                "auth_mode" -> preferences[Keys.AUTH_MODE] = value
            }
        }
        Log.d("ProfileRepository", "Updated $key with value $value")
        Log.d("ProfileRepository", "Updated preferences: ${context.dataStore.data.first()}")
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
}
