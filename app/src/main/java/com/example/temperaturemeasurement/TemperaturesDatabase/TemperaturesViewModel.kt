package com.example.temperaturemeasurement

import androidx.lifecycle.*
import com.example.temperaturemeasurement.TemperaturesDatabase.Temperatures
import com.example.temperaturemeasurement.TemperaturesDatabase.TemperaturesRepository
import kotlinx.coroutines.launch

class TemperaturesViewModel(private val repository: TemperaturesRepository) : ViewModel() {
    val allTemperatures: LiveData<List<Temperatures>> = repository.allTemperatures.asLiveData()
    val getName: Array<String> = repository.SensorName

    fun getTemperaturesList(): List<Temperatures> {
        return repository.getAllData()
    }

    fun insert(temperatures: Temperatures) = viewModelScope.launch {
        repository.insert(temperatures)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

}

class TemperaturesViewModelFactory(private val repository: TemperaturesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemperaturesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TemperaturesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}