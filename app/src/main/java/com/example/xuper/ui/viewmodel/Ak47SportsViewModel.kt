package com.example.xuper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xuper.data.Ak47Event
import com.example.xuper.data.Ak47SportsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Ak47SportsViewModel(application: Application) : AndroidViewModel(application) {

    private val ak47Manager = Ak47SportsManager(application)

    private val _events = MutableStateFlow<List<Ak47Event>>(emptyList())
    val events: StateFlow<List<Ak47Event>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lastFetchTimeString = MutableStateFlow("")
    val lastFetchTimeString: StateFlow<String> = _lastFetchTimeString.asStateFlow()

    init {
        loadEvents(forceRefresh = false)
    }

    fun loadEvents(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val list = ak47Manager.fetchEvents(forceRefresh)
                _events.value = list
                _lastFetchTimeString.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando eventos de AK47Sports: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
