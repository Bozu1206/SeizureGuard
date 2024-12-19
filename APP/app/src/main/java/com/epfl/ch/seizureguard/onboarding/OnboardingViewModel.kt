package com.epfl.ch.seizureguard.onboarding

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "onboarding_preferences")
private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

@SuppressLint("StaticFieldLeak")
class OnboardingViewModel(private val context: Context) : ViewModel() {

    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding

    init {
        checkOnboardingStatus()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val preferences = context.dataStore.data.first()
            _showOnboarding.value = preferences[ONBOARDING_COMPLETED_KEY] != true
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[ONBOARDING_COMPLETED_KEY] = true
            }
            _showOnboarding.value = false
        }
    }

    /* Only for Debug purpose */
    fun resetOnboarding(profileViewModel: ProfileViewModel) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences.remove(ONBOARDING_COMPLETED_KEY)
            }
            _showOnboarding.value = true

            profileViewModel.resetProfile()
        }


    }
}

class OnboardingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
