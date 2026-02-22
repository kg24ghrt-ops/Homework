package com.debk007.template.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debk007.template.model.Anime // We'll define this next
import com.debk007.template.repository.Repository
import com.debk007.template.util.ApiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    // Main list for Home Screen
    private val _animeListState = MutableStateFlow<ApiState<List<Anime>>>(ApiState.Loading)
    val animeListState = _animeListState.asStateFlow()

    // Results for Search Screen
    private val _searchResultsState = MutableStateFlow<ApiState<List<Anime>>>(ApiState.Idle)
    val searchResultsState = _searchResultsState.asStateFlow()

    init {
        getTrendingAnime()
    }

    private fun getTrendingAnime() {
        viewModelScope.launch(Dispatchers.IO) {
            _animeListState.value = ApiState.Loading
            _animeListState.value = repository.getTrendingAnime()
        }
    }

    fun searchAnime(query: String) {
        if (query.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _searchResultsState.value = ApiState.Loading
            _searchResultsState.value = repository.searchAnime(query)
        }
    }

    // Helper for DetailScreen to find anime from currently loaded lists
    fun getAnimeById(id: String?): Anime? {
        val currentList = (_animeListState.value as? ApiState.Success)?.data ?: emptyList()
        val searchList = (_searchResultsState.value as? ApiState.Success)?.data ?: emptyList()
        return (currentList + searchList).find { it.id == id }
    }
}
