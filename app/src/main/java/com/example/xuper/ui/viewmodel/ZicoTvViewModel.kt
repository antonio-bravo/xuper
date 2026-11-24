package com.example.xuper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xuper.data.ZicoTvChannel
import com.example.xuper.data.ZicoTvManager
import com.example.xuper.data.ZicoTvTokenInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ZicoTvViewModel(application: Application) : AndroidViewModel(application) {

    private val zicoTvManager = ZicoTvManager(application)

    private val _channels = MutableStateFlow<List<ZicoTvChannel>>(emptyList())
    val channels: StateFlow<List<ZicoTvChannel>> = _channels.asStateFlow()

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
        loadChannels(forceRefresh = false)
    }

    fun loadChannels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val list = zicoTvManager.fetchChannels(forceRefresh)
                _channels.value = list
                _lastFetchTimeString.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando canales de ZicoTV: ${e.message}"
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

    suspend fun getStreamToken(channelId: Int, forceRefresh: Boolean = false): ZicoTvTokenInfo {
        return zicoTvManager.getTokenForChannel(channelId, forceRefresh)
    }
}
