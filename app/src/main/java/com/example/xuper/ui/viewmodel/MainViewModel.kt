package com.example.xuper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xuper.data.ChannelRepository
import com.example.xuper.model.Channel
import com.example.xuper.model.M3UList
import com.example.xuper.data.M3UParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: ChannelRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allChannels: StateFlow<List<Channel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = repository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedListName = MutableStateFlow("Todas las listas")
    val selectedListName = _selectedListName.asStateFlow()

    val filteredChannels: StateFlow<List<Channel>> = combine(
        searchQuery,
        selectedCategory,
        selectedListName,
        allChannels, // This triggers refresh when DB changes
    ) { query, category, listName, _ ->
        Triple(query, category, listName)
    }.flatMapLatest { (query, category, listName) ->
        repository.getFilteredChannels(query, category, listName)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedListName(name: String) {
        _selectedListName.value = name
        _selectedCategory.value = "Todos" // Reset category when switching lists
    }

    fun refreshChannels(m3uLists: List<M3UList>) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                for (source in m3uLists) {
                    val fetched = M3UParser.fetchAndParse(source.url, source.name)
                    repository.refreshChannels(fetched, source.name)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar canales: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel, !channel.isFavorite)
        }
    }

    fun searchChannels(query: String): Flow<List<Channel>> {
        return repository.searchChannels(query)
    }
}
