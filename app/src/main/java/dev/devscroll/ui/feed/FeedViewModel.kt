package dev.devscroll.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devscroll.data.model.Article
import dev.devscroll.data.model.User
import dev.devscroll.data.repository.ArticleRepository
import dev.devscroll.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.devscroll.ui.PaginationConfig
import javax.inject.Inject

enum class SortOrder { NEWEST, TRENDING }

data class FeedUiState(
    val articles: List<Article> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
    val activeCategoryFilters: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val isBatchLimitReached: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
)

private data class LoadState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
    val searchQuery: String? = null,
)



@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val userState: StateFlow<User?> = authRepository.userState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val savingArticles: StateFlow<Set<Long>> = repository.observeSavingIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    private val categoryFilters = MutableStateFlow<Set<String>>(emptySet())
    private val sortOrder = MutableStateFlow(SortOrder.NEWEST)
    private val loadState = MutableStateFlow(LoadState())
    private var currentPage = 0

    private val articleSource = loadState
        .map { it.searchQuery }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query == null) repository.observeArticles()
            else repository.observeSearchResults()
        }

    // Search query lives in loadState, but whether search is active only needs to
    // gate the filter/sort below. Track it separately so load-flag changes
    // (isRefreshing/isLoadingMore) don't force a re-sort of the whole list.
    private val searchActive = loadState
        .map { it.searchQuery != null }
        .distinctUntilChanged()

    // Heavy work (filter + sort) depends only on the data, the active filters,
    // the sort order, and whether search is active. It is deliberately kept out
    // of the combine with loadState so that pagination/refresh flag toggles do
    // not re-run the O(n log n) sort over a growing list while the user scrolls.
    private val processedArticles = combine(
        articleSource,
        categoryFilters,
        sortOrder,
        searchActive,
    ) { articles, filters, sort, isSearchActive ->
        if (isSearchActive) {
            articles
        } else {
            articles
                .filter { filters.isEmpty() || it.categories.any { cat -> cat in filters } }
                .let { list ->
                    when (sort) {
                        SortOrder.NEWEST -> list.sortedByDescending { it.publishedOn }
                        SortOrder.TRENDING -> list.sortedByDescending { it.hitCount }
                    }
                }
        }
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<FeedUiState> = combine(
        processedArticles,
        categoryFilters,
        sortOrder,
        loadState,
    ) { articles, filters, sort, load ->
        val isSearchActive = load.searchQuery != null
        val limitReached = (currentPage + 1) % PaginationConfig.MAX_PAGES_PER_BATCH == 0
        FeedUiState(
            articles = articles,
            isRefreshing = load.isRefreshing,
            isLoadingMore = load.isLoadingMore,
            hasNext = load.hasNext,
            error = load.error,
            activeCategoryFilters = filters,
            sortOrder = sort,
            isBatchLimitReached = limitReached,
            isSearchActive = isSearchActive,
            searchQuery = load.searchQuery ?: "",
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FeedUiState(),
        )

    init {
        refresh()
    }

    fun refresh() {
        val query = loadState.value.searchQuery
        if (query != null) {
            search(query)
            return
        }
        viewModelScope.launch {
            currentPage = 0
            loadState.update { it.copy(isRefreshing = true, error = null) }
            repository.clearFeedArticles()
            repository.fetchArticles(page = 0, sortType = sortOrder.value.name)
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    loadState.update { it.copy(isRefreshing = false, error = e.message) }
                }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            currentPage = 0
            loadState.update { it.copy(isRefreshing = true, error = null, searchQuery = query) }
            repository.clearSearchResults()
            repository.search(query = query, page = 0)
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    loadState.update { it.copy(isRefreshing = false, error = e.message) }
                }
        }
    }

    fun clearSearch() {
        viewModelScope.launch {
            repository.clearSearchResults()
            currentPage = 0
            loadState.update { it.copy(searchQuery = null, hasNext = false, error = null) }
        }
    }

    fun loadNextPage() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext) return
        val limitReached = (currentPage + 1) % PaginationConfig.MAX_PAGES_PER_BATCH == 0
        if (limitReached) return

        viewModelScope.launch {
            currentPage++
            loadState.update { it.copy(isLoadingMore = true) }
            val result = if (state.searchQuery != null) {
                repository.search(query = state.searchQuery, page = currentPage)
            } else {
                repository.fetchArticles(page = currentPage, sortType = sortOrder.value.name)
            }
            result
                .onSuccess { page ->
                    loadState.update { it.copy(
                        isLoadingMore = false,
                        hasNext = page.hasNext,
                    )}
                }
                .onFailure { e ->
                    currentPage--
                    loadState.update { it.copy(isLoadingMore = false, error = e.message) }
                }
        }
    }

    fun loadNextBatch() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext) return
        viewModelScope.launch {
            currentPage++
            loadState.update { it.copy(isRefreshing = true, error = null) }
            if (state.searchQuery == null) {
                repository.clearFeedArticles()
            } else {
                repository.clearSearchResults()
            }
            val result = if (state.searchQuery != null) {
                repository.search(query = state.searchQuery, page = currentPage)
            } else {
                repository.fetchArticles(page = currentPage, sortType = sortOrder.value.name)
            }
            result
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    currentPage--
                    loadState.update { it.copy(isRefreshing = false, error = e.message) }
                }
        }
    }

    fun resetAndRefresh() {
        viewModelScope.launch {
            currentPage = 0
            loadState.update { it.copy(isRefreshing = true, error = null, searchQuery = null) }
            repository.clearFeedArticles()
            repository.clearSearchResults()
            repository.fetchArticles(page = 0, sortType = sortOrder.value.name)
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    loadState.update { it.copy(isRefreshing = false, error = e.message) }
                }
        }
    }

    fun toggleSave(id: Long) { viewModelScope.launch { repository.toggleSave(id) } }
    fun markRead(id: Long) { viewModelScope.launch { repository.markRead(id) } }

    fun setFilter(tags: Set<String>) { categoryFilters.value = tags }
    fun clearFilter() { categoryFilters.value = emptySet() }

    fun toggleSort() {
        sortOrder.update { if (it == SortOrder.NEWEST) SortOrder.TRENDING else SortOrder.NEWEST }
        resetAndRefresh()
    }

    fun clearError() { loadState.update { it.copy(error = null) } }
}
