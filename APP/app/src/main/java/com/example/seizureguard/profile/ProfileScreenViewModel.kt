import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
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

    init {
        loadProfile()
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
                Profile(userId, name, email, birthdate, uri, pwd, epi_type)
            }.collect { profile ->
                _userId.value = profile.uid
                _userName.value = profile.name
                _userEmail.value = profile.email
                _birthdate.value = profile.birthdate
                _profilePictureUri.value =
                    if (profile.uri.isNotEmpty()) Uri.parse(profile.uri) else null
                _pwd.value = profile.pwd
                _epi_type.value = profile.epi_type

                Log.d("ProfileViewModel", "Loaded profile: $profile")
            }
        }
    }


    fun saveProfile(name: String, email: String, birthdate: String, uri: Uri? = null, pwd: String, epi_type: String) {
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
    val epi_type: String
)