package com.example.temperaturemeasurement.UniqueSensorsDatabase

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class UniqueSensorsViewModel(private val repository: UniqueSensorsRepository) : ViewModel() {
    val allSensors: LiveData<List<UniqueSensors>> = repository.allSensors.asLiveData()
    val getPathName: Array<String> = repository.PathName
    val getPathTemp: Array<String> = repository.PathTemp
    val getToRead: Array<Boolean> = repository.ToRead
    val getName: Array<String> = repository.SensorName

    fun insert(sensors: UniqueSensors) = viewModelScope.launch {
        repository.insert(sensors)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

    fun update(valName: String, valTemp: String) = viewModelScope.launch {
        repository.update(valName, valTemp)
    }

    fun updateToRead(valName: String, valRead: Boolean) = viewModelScope.launch {
        repository.updateToRead(valName, valRead)
    }

    fun updateAll(sensors: UniqueSensors) = viewModelScope.launch {
        repository.updateAll(sensors)
    }

    fun getToReadSingle(valName: String) = viewModelScope.launch  {
        repository.getToReadSingle(valName)
    }

}

class UniqueSensorsViewModelFactory(private val repository: UniqueSensorsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UniqueSensorsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UniqueSensorsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}