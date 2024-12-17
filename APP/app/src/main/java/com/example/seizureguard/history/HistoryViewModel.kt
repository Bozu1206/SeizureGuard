package com.example.seizureguard.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.seizureguard.seizure_event.SeizureDao
import com.example.seizureguard.seizure_event.SeizureDatabase
import com.example.seizureguard.seizure_event.SeizureEntity
import com.example.seizureguard.seizure_event.SeizureEvent
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private var _seizures =
        MutableLiveData<MutableList<SeizureEvent>>(mutableListOf()) // create a list object for storing past sessions
    val seizures: LiveData<MutableList<SeizureEvent>>
        get() = _seizures

    private val database: SeizureDao = SeizureDatabase.getInstance(application).seizureDao

    fun removeSeizure(event: SeizureEvent) {
        viewModelScope.launch {
            database.deleteByTimestamp(event.timestamp)
            readHistory()
        }
    }

    fun editSeizure(newSeizure: SeizureEvent, oldSeizure: SeizureEvent) {
        viewModelScope.launch {
            database.updateByTimestamp(
                newSeizure.type,
                newSeizure.duration,
                newSeizure.severity,
                newSeizure.triggers,
                oldSeizure.timestamp
            )
            readHistory()
        }
    }

    fun readHistory() {
        viewModelScope.launch {
            val read_seizures = database.getAllSeizureEvents()
            var seizures_list: MutableList<SeizureEvent> = mutableListOf()
            read_seizures.forEach {
                var new_seizure =
                    SeizureEvent(it.type, it.duration, it.severity, it.triggers, it.timestamp)
                seizures_list.add(new_seizure)
            }
            _seizures.value = seizures_list
        }
    }
}
