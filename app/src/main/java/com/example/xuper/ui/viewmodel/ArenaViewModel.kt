package com.example.xuper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xuper.data.ArenaParser
import com.example.xuper.model.ArenaEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArenaViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<ArenaEvent>>(emptyList())
    val events: StateFlow<List<ArenaEvent>> = _events.asStateFlow()

    private val _streams = MutableStateFlow<Map<String, String>>(emptyMap())
    val streams: StateFlow<Map<String, String>> = _streams.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedSource = MutableStateFlow(ArenaParser.sources.first())
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

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
    fun loadData(autoScan: Boolean = false) {
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
