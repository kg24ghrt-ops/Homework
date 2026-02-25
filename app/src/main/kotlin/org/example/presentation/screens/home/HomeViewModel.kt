// presentation/screens/home/HomeViewModel.kt
package com.openanimelib.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openanimelib.domain.model.Anime
import com.openanimelib.domain.usecase.anime.*
import com.openanimelib.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val trending: List<Anime> = emptyList(),
    val seasonal: List<Anime> = emptyList(),
    val popular: List<Anime> = emptyList(),
    val recentlyUpdated: List<Anime> = emptyList(),
    val randomPick: Anime? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTrendingAnime: GetTrendingAnimeUseCase,
    private val getSeasonalAnime: GetSeasonalAnimeUseCase,
    private val getRandomAnime: GetRandomAnimeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load trending
            launch {
                getTrendingAnime.invokeAsList(page = 1, perPage = 20).collect { result ->
                    when (result) {
                        is Resource.Success -> _uiState.update {
                            it.copy(trending = result.data ?: emptyList())
                        }
                        is Resource.Error -> _uiState.update {
                            it.copy(error = result.message)
                        }
                        is Resource.Loading -> {}
                    }
                }
            }

            // Load seasonal
            launch {
                getSeasonalAnime().collect { result ->
                    when (result) {
                        is Resource.Success -> _uiState.update {
                            it.copy(seasonal = result.data ?: emptyList())
                        }
                        is Resource.Error -> {}
                        is Resource.Loading -> {}
                    }
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadHomeData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun pickRandomAnime() {
        viewModelScope.launch {
            val result = getRandomAnime()
            if (result is Resource.Success) {
                _uiState.update { it.copy(randomPick = result.data) }
            }
        }
    }
}