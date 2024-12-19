package com.example.seizureguard.dl

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.seizureguard.dl.metrics.Metrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "user_model")

private val USER_ID_KEY = stringPreferencesKey("user_id")
private val MODEL_METRICS = stringPreferencesKey("model_metrics")

class InferenceModelViewModel(private val application: Application) :
    AndroidViewModel(application) {
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId

    private val _modelMetrics = MutableStateFlow(Metrics())
    val modelMetrics: StateFlow<Metrics> = _modelMetrics

    init {
        loadUserId()
        loadModelMetrics()
    }

    fun updateModelMetrics(metrics: Metrics) {
        viewModelScope.launch {
            application.dataStore.edit { preferences ->
                preferences[MODEL_METRICS] = metrics.toString()
            }
        }
    }

    private fun loadModelMetrics() {
       viewModelScope.launch {
                val metrics = application.dataStore.data.map {
                    it[MODEL_METRICS] ?: ""
                }
                _modelMetrics.value = Metrics.fromString(metrics)
            }
    }

    private fun loadUserId() {
        viewModelScope.launch {
            val userId = application.dataStore.data.map {
                it[USER_ID_KEY] ?: ""
            }
            _userId.value = userId.toString()
        }
    }

}