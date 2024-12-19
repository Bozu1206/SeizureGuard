import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "user_profile")

private val USER_ID_KEY = stringPreferencesKey("user_id")
private val USER_NAME_KEY = stringPreferencesKey("user_name")
private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
private val USER_DOB = stringPreferencesKey("user_dob")
private val USER_PP = stringPreferencesKey("user_pp")
private val USER_PWD = stringPreferencesKey("user_pwd")
private val USER_EPI_TYPE = stringPreferencesKey("user_epi_type")
private val EMERGENCY_CONTACTS_KEY = stringPreferencesKey("emergency_contacts")
private val AUTH_MODE_KEY = stringPreferencesKey("auth_mode")
private val IS_BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("is_biometric_enabled")
private val IS_TRAINING_ENABLED_KEY = booleanPreferencesKey("is_training_enabled")


@SuppressLint("StaticFieldLeak")
class ProfileViewModel(private val context: Context) : ViewModel() {
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail

    private val _birthdate = MutableStateFlow("")
    val birthdate: StateFlow<String> = _birthdate

    private val _profilePictureUri = MutableStateFlow<Uri?>(null)
    val profilePictureUri: StateFlow<Uri?> = _profilePictureUri

    private val _pwd = MutableStateFlow("")
    val pwd: StateFlow<String> = _pwd

    private val _epi_type = MutableStateFlow("")
    val epi_type: StateFlow<String> = _epi_type

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts

    private val _auth_mode = MutableStateFlow("")
    val auth_mode: StateFlow<String> = _auth_mode

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    private val _isTrainingEnabled = MutableStateFlow(false)
    val isTrainingEnabled: StateFlow<Boolean> = _isTrainingEnabled

    init {
        loadProfile()
        loadEmergencyContacts()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            context.dataStore.data.map { preferences ->
                val userId = preferences[USER_ID_KEY] ?: UUID.randomUUID().toString()
                val name = preferences[USER_NAME_KEY] ?: ""
                val email = preferences[USER_EMAIL_KEY] ?: ""
                val birthdate = preferences[USER_DOB] ?: ""
                val uri = preferences[USER_PP] ?: ""
                val pwd = preferences[USER_PWD] ?: ""
                val epi_type = preferences[USER_EPI_TYPE] ?: ""
                val auth_mode = preferences[AUTH_MODE_KEY] ?: ""
                val isBiometricEnabled = preferences[IS_BIOMETRIC_ENABLED_KEY] ?: false
                val isTrainingEnabled = preferences[IS_TRAINING_ENABLED_KEY] ?: false
                Profile(userId, name, email, birthdate, uri, pwd, epi_type, auth_mode, isBiometricEnabled, isTrainingEnabled)
            }.collect { profile ->
                _userId.value = profile.uid
                _userName.value = profile.name
                _userEmail.value = profile.email
                _birthdate.value = profile.birthdate
                _profilePictureUri.value =
                    if (profile.uri.isNotEmpty()) Uri.parse(profile.uri) else null
                _pwd.value = profile.pwd
                _epi_type.value = profile.epi_type
                _auth_mode.value = profile.auth_mode
                _isBiometricEnabled.value = profile.isBiometricEnabled
                _isTrainingEnabled.value = profile.isTrainingEnabled
                Log.d("ProfileViewModel", "Loaded profile: $profile")
            }
        }
    }

    private fun loadEmergencyContacts() {
        viewModelScope.launch {
            context.dataStore.data.map { preferences ->
                preferences[EMERGENCY_CONTACTS_KEY]?.let { json ->
                    val contacts = try {
                        val type = object :
                            com.google.gson.reflect.TypeToken<List<EmergencyContact>>() {}.type
                        com.google.gson.Gson().fromJson(json, type)
                    } catch (e: Exception) {
                        emptyList<EmergencyContact>()
                    }
                    contacts
                } ?: emptyList()
            }.collect { contacts ->
                _emergencyContacts.value = contacts
            }
        }
    }

    fun saveEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                val updatedContacts = _emergencyContacts.value.toMutableList()
                if (updatedContacts.size < 5) {
                    updatedContacts.add(contact)
                    val json = com.google.gson.Gson().toJson(updatedContacts)
                    context.dataStore.edit { preferences ->
                        preferences[EMERGENCY_CONTACTS_KEY] = json
                    }
                    _emergencyContacts.value = updatedContacts

                } else {
                    Log.e("ProfileViewModel", "Cannot add more than 5 emergency contacts")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error saving emergency contact: ${e.message}")
            }
        }
    }


    fun removeEmergencyContact(phone: String) {
        viewModelScope.launch {
            try {
                val updatedContacts = _emergencyContacts.value.filter { it.phone != phone }

                val json = com.google.gson.Gson().toJson(updatedContacts)

                context.dataStore.edit { preferences ->
                    preferences[EMERGENCY_CONTACTS_KEY] = json
                }

                _emergencyContacts.value = updatedContacts
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error removing emergency contact: ${e.message}")
            }
        }
    }

    fun isEmpty(): Boolean =
        _userName.value.isEmpty() || _userEmail.value.isEmpty() || _birthdate.value.isEmpty() || _profilePictureUri.value == null


    fun saveProfile(
        name: String,
        email: String,
        birthdate: String,
        uri: Uri? = null,
        pwd: String,
        epi_type: String
    ) {
        viewModelScope.launch {
            try {
                context.dataStore.edit { preferences ->
                    val userId = preferences[USER_ID_KEY] ?: UUID.randomUUID().toString()
                    preferences[USER_ID_KEY] = userId
                    preferences[USER_NAME_KEY] = name
                    preferences[USER_EMAIL_KEY] = email
                    preferences[USER_DOB] = birthdate
                    preferences[USER_PP] = uri?.toString() ?: ""
                    preferences[USER_PWD] = pwd
                    preferences[USER_EPI_TYPE] = epi_type
                }
                _userName.value = name
                _userEmail.value = email
                _birthdate.value = birthdate
                _profilePictureUri.value = uri
                _pwd.value = pwd
                _epi_type.value = epi_type
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error saving profile: ${e.message}")
            }
        }
    }


    fun updateUserName(name: String) {
        _userName.value = name
    }

    fun updateUserMail(email: String) {
        _userEmail.value = email
    }

    fun updateEpilepsyType(epi_type: String) {
        _epi_type.value = epi_type
    }

    fun persistProfile() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = UUID.randomUUID().toString()
                preferences[USER_NAME_KEY] = userName.value
                preferences[USER_EMAIL_KEY] = userEmail.value
                preferences[USER_DOB] = birthdate.value
                preferences[USER_PP] = profilePictureUri.value?.toString() ?: ""
                preferences[USER_PWD] = pwd.value
                preferences[USER_EPI_TYPE] = epi_type.value
            }
        }
    }

    private fun resetEmergencyContacts() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences.remove(EMERGENCY_CONTACTS_KEY)
            }
            _emergencyContacts.value = emptyList()
        }
    }


    fun resetProfile() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences.remove(USER_ID_KEY)
                preferences.remove(USER_NAME_KEY)
                preferences.remove(USER_EMAIL_KEY)
                preferences.remove(USER_DOB)
                preferences.remove(USER_PP)
                preferences.remove(USER_PWD)
                preferences.remove(USER_EPI_TYPE)
            }
        }

        resetEmergencyContacts()
    }

    fun saveAuthPreference(mode: String) {
        if (mode == "biometric") {
            viewModelScope.launch {
                _isBiometricEnabled.value = true
                context.dataStore.edit { preferences ->
                    preferences[AUTH_MODE_KEY] = "biometric"
                    preferences[IS_BIOMETRIC_ENABLED_KEY] = true
                }
            }
        } else {
            viewModelScope.launch {
                _isBiometricEnabled.value = false
                context.dataStore.edit { preferences ->
                    preferences[AUTH_MODE_KEY] = "password"
                    preferences[IS_BIOMETRIC_ENABLED_KEY] = false
                }
            }
        }
    }

    fun saveTrainingPreference(bool: Boolean) {
        viewModelScope.launch {
            _isTrainingEnabled.value = bool
            context.dataStore.edit { preferences ->
                preferences[IS_TRAINING_ENABLED_KEY] = bool
            }
        }
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class Profile(
    val uid: String,
    val name: String,
    val email: String,
    val birthdate: String,
    val uri: String,
    val pwd: String,
    val epi_type: String,
    val auth_mode: String,
    val isBiometricEnabled: Boolean,
    val isTrainingEnabled: Boolean
)

data class EmergencyContact(
    val name: String,
    val phone: String,
    val photoUri: String? = null
)
