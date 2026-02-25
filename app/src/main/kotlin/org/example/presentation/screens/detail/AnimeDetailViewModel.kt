// presentation/screens/detail/AnimeDetailViewModel.kt
package com.openanimelib.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openanimelib.domain.model.*
import com.openanimelib.domain.usecase.anime.GetAnimeDetailUseCase
import com.openanimelib.domain.usecase.anime.GetAnimeSourcesUseCase
import com.openanimelib.domain.usecase.favorites.ToggleFavoriteUseCase
import com.openanimelib.domain.usecase.watchlist.*
import com.openanimelib.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val detail: AnimeDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isInWatchlist: Boolean = false,
    val isFavorite: Boolean = false,
    val watchlistStatus: WatchStatus? = null,
    val currentEpisode: Int = 0,
    val showWatchlistDialog: Boolean = false
)

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnimeDetail: GetAnimeDetailUseCase,
    private val getAnimeSources: GetAnimeSourcesUseCase,
    private val addToWatchlist: AddToWatchlistUseCase,
    private val removeFromWatchlist: RemoveFromWatchlistUseCase,
    private val updateProgress: UpdateProgressUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val animeId: Int = savedStateHandle.get<Int>("animeId") ?: 0

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadAnimeDetail()
        observeWatchlistStatus()
    }

    private fun loadAnimeDetail() {
        viewModelScope.launch {
            getAnimeDetail(animeId).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            detail = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    private fun observeWatchlistStatus() {
        viewModelScope.launch {
            watchlistRepository.getWatchlistItem(animeId).collect { item ->
                _uiState.update {
                    it.copy(
                        isInWatchlist = item != null,
                        watchlistStatus = item?.status,
                        currentEpisode = item?.currentEpisode ?: 0
                    )
                }
            }
        }
    }

    fun onAddToWatchlist(status: WatchStatus) {
        viewModelScope.launch {
            addToWatchlist(animeId, status)
            _uiState.update { it.copy(showWatchlistDialog = false) }
        }
    }

    fun onRemoveFromWatchlist() {
        viewModelScope.launch {
            removeFromWatchlist(animeId)
        }
    }

    fun onUpdateEpisode(episode: Int) {
        viewModelScope.launch {
            updateProgress(animeId, episode)
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            toggleFavorite(animeId)
            _uiState.update { it.copy(isFavorite = !it.isFavorite) }
        }
    }

    fun showWatchlistDialog() {
        _uiState.update { it.copy(showWatchlistDialog = true) }
    }

    fun dismissWatchlistDialog() {
        _uiState.update { it.copy(showWatchlistDialog = false) }
    }
}