package com.example.xuper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xuper.data.ChannelRepository
import com.example.xuper.model.Channel
import com.example.xuper.model.M3UList
import com.example.xuper.data.M3UParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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

    private val _selectedCategory = MutableStateFlow(ChannelRepository.ALL_CATEGORIES)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedListName = MutableStateFlow(ChannelRepository.ALL_LISTS)
    val selectedListName = _selectedListName.asStateFlow()

    val availableCategories: StateFlow<List<String>> = combine(
        allChannels,
        selectedListName
    ) { channels, listName ->
        val base = if (listName == ChannelRepository.ALL_LISTS) {
            channels
        } else {
            channels.filter { it.sourceListName == listName }
        }
        base.asSequence().map { it.category }.distinct().sorted().toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val filteredChannels: StateFlow<List<Channel>> = combine(
        searchQuery.debounce(300),
        selectedCategory,
        selectedListName
    ) { query, category, listName ->
        Triple(query, category, listName)
    }.flatMapLatest { (query, category, listName) ->
        repository.getFilteredChannels(query, category, listName)
    }.map { list ->
        list.distinctBy { it.url + it.name + it.category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedListName(name: String) {
        _selectedListName.value = name
        _selectedCategory.value = ChannelRepository.ALL_CATEGORIES // Reset category when switching lists
    }

    fun refreshChannels(m3uLists: List<M3UList>) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val semaphore = Semaphore(4)
                coroutineScope {
                    m3uLists.map { source ->
                        async {
                            if (source.enabled) {
                                semaphore.withPermit {
                                    val fetched = M3UParser.fetchAndParse(source.url, source.name)
                                    repository.refreshChannels(fetched, source.name)
                                }
                            } else {
                                repository.clearChannelsBySource(source.name)
                            }
                        }
                    }.awaitAll()
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
