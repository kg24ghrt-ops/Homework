package com.openanimelib.presentation.screens.search
// presentation/screens/search/SearchViewModel.kt
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAnime: SearchAnimeUseCase,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    private val _selectedYear = MutableStateFlow<Int?>(null)
    private val _selectedFormat = MutableStateFlow<String?>(null)
    private val _sortBy = MutableStateFlow("POPULARITY_DESC")

    val searchHistory: Flow<List<String>> = searchHistoryDao.getRecentSearches()
        .map { entities -> entities.map { it.query } }

    val searchResults: Flow<PagingData<Anime>> = combine(
        _searchQuery.debounce(300),
        _selectedGenre,
        _selectedYear,
        _selectedFormat,
        _sortBy
    ) { query, genre, year, format, sort ->
        SearchParams(query, genre, year, format, sort)
    }.flatMapLatest { params ->
        if (params.query.isBlank() && params.genre == null) {
            flowOf(PagingData.empty())
        } else {
            searchAnime(
                query = params.query,
                genre = params.genre,
                year = params.year,
                format = params.format,
                sort = params.sort
            )
        }
    }.cachedIn(viewModelScope)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isNotBlank()) {
                searchHistoryDao.insert(
                    SearchHistoryEntity(query = query)
                )
            }
        }
    }

    fun onGenreSelected(genre: String?) { _selectedGenre.value = genre }
    fun onYearSelected(year: Int?) { _selectedYear.value = year }
    fun onFormatSelected(format: String?) { _selectedFormat.value = format }
    fun onSortChanged(sort: String) { _sortBy.value = sort }

    fun clearHistory() {
        viewModelScope.launch { searchHistoryDao.clearAll() }
    }

    private data class SearchParams(
        val query: String,
        val genre: String?,
        val year: Int?,
        val format: String?,
        val sort: String
    )
}