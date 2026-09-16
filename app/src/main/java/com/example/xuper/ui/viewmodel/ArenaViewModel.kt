package com.example.xuper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xuper.data.ArenaParser
import com.example.xuper.model.ArenaEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArenaViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<ArenaEvent>>(emptyList())
    val events: StateFlow<List<ArenaEvent>> = _events.asStateFlow()

    private val _streams = MutableStateFlow<Map<String, String>>(emptyMap())
    val streams: StateFlow<Map<String, String>> = _streams.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedSource = MutableStateFlow(ArenaParser.sources.first())
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

    private val _lastFetchTimeString = MutableStateFlow("")
    val lastFetchTimeString: StateFlow<String> = _lastFetchTimeString.asStateFlow()

    private var lastFetchTime = 0L
    private val CACHE_DURATION = 10 * 60 * 1000L // 10 minutos

    init {
        loadData(autoScan = true)
    }

    fun setSelectedSource(url: String) {
        _selectedSource.value = url
        loadData(autoScan = false)
    }

    /**
     * @param autoScan when true, tries every source in order and stops as soon as one
     * yields events, instead of only fetching the currently selected source.
     */
    fun loadData(autoScan: Boolean = false, forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!forceRefresh && _events.value.isNotEmpty() && (currentTime - lastFetchTime < CACHE_DURATION)) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (autoScan) {
                    var found = false
                    for (source in ArenaParser.sources) {
                        val (events, streams) = ArenaParser.fetchArenaData(source)
                        if (events.isNotEmpty()) {
                            _selectedSource.value = source
                            _events.value = events
                            _streams.value = streams
                            found = true
                            lastFetchTime = System.currentTimeMillis()
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            _lastFetchTimeString.value = sdf.format(Date(lastFetchTime))
                            break
                        }
                    }
                    if (!found) {
                        _events.value = emptyList()
                        _streams.value = emptyMap()
                    }
                } else {
                    val (events, streams) = ArenaParser.fetchArenaData(_selectedSource.value)
                    _events.value = events
                    _streams.value = streams
                    if (events.isNotEmpty()) {
                        lastFetchTime = System.currentTimeMillis()
                    }
                }
                if (lastFetchTime > 0L) {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    _lastFetchTimeString.value = sdf.format(Date(lastFetchTime))
                }
            } catch (e: Exception) {
                _events.value = emptyList()
                _streams.value = emptyMap()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
