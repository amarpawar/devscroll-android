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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val activeBlogFilter: Long? = null,
    val activeBlogName: String? = null,
)

private data class LoadState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
    val searchQuery: String? = null,
    // Lives here rather than in a plain field so uiState actually recomputes when
    // the page changes — isBatchLimitReached is derived from it.
    val page: Int = 0,
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
    private val blogFilter = MutableStateFlow<Long?>(null)
    private val sortOrder = MutableStateFlow(SortOrder.NEWEST)
    private val loadState = MutableStateFlow(LoadState())

    @OptIn(ExperimentalCoroutinesApi::class)
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
        blogFilter,
    ) { articles, filters, sort, isSearchActive, blogId ->
        if (isSearchActive) {
            articles
        } else {
            articles
                .filter { blogId == null || it.blogId == blogId }
                .filter { filters.isEmpty() || it.categories.any { cat -> cat in filters } }
                .let { list ->
                    when (sort) {
                        SortOrder.NEWEST -> list.sortedByDescending { it.publishedOn }
                        SortOrder.TRENDING -> list.sortedByDescending { it.hitCount }
                    }
                }
        }
    }.flowOn(Dispatchers.Default)

    private val activeBlogState = combine(
        blogFilter,
        repository.observeSources()
    ) { blogId, sources ->
        val name = if (blogId == null) null else sources.find { it.blogId == blogId }?.blogName
        blogId to name
    }

    val uiState: StateFlow<FeedUiState> = combine(
        processedArticles,
        categoryFilters,
        sortOrder,
        loadState,
        activeBlogState,
    ) { articles, filters, sort, load, blogState ->
        val isSearchActive = load.searchQuery != null
        val limitReached = (load.page + 1) % PaginationConfig.MAX_PAGES_PER_BATCH == 0
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
            activeBlogFilter = blogState.first,
            activeBlogName = blogState.second,
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
            loadState.update { it.copy(isRefreshing = true, error = null, page = 0) }
            repository.clearFeedArticles()
            repository.fetchArticles(page = 0, sortType = sortOrder.value.name, blogId = blogFilter.value)
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
            loadState.update { it.copy(isRefreshing = true, error = null, searchQuery = query, page = 0) }
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
            loadState.update { it.copy(searchQuery = null, hasNext = false, error = null, page = 0) }
        }
    }

    fun loadNextPage() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext) return
        val limitReached = (state.page + 1) % PaginationConfig.MAX_PAGES_PER_BATCH == 0
        if (limitReached) return

        val nextPage = state.page + 1
        viewModelScope.launch {
            loadState.update { it.copy(isLoadingMore = true, page = nextPage) }
            val result = if (state.searchQuery != null) {
                repository.search(query = state.searchQuery, page = nextPage)
            } else {
                repository.fetchArticles(page = nextPage, sortType = sortOrder.value.name, blogId = blogFilter.value)
            }
            result
                .onSuccess { page ->
                    loadState.update { it.copy(
                        isLoadingMore = false,
                        hasNext = page.hasNext,
                    )}
                }
                .onFailure { e ->
                    loadState.update { it.copy(isLoadingMore = false, error = e.message, page = state.page) }
                }
        }
    }

    fun loadNextBatch() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext) return
        val nextPage = state.page + 1
        viewModelScope.launch {
            loadState.update { it.copy(isRefreshing = true, error = null, page = nextPage) }
            if (state.searchQuery == null) {
                repository.clearFeedArticles()
            } else {
                repository.clearSearchResults()
            }
            val result = if (state.searchQuery != null) {
                repository.search(query = state.searchQuery, page = nextPage)
            } else {
                repository.fetchArticles(page = nextPage, sortType = sortOrder.value.name, blogId = blogFilter.value)
            }
            result
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    loadState.update { it.copy(isRefreshing = false, error = e.message, page = state.page) }
                }
        }
    }

    fun resetAndRefresh() {
        viewModelScope.launch {
            loadState.update { it.copy(isRefreshing = true, error = null, searchQuery = null, page = 0) }
            repository.clearFeedArticles()
            repository.clearSearchResults()
            repository.fetchArticles(page = 0, sortType = sortOrder.value.name, blogId = blogFilter.value)
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    loadState.update { it.copy(isRefreshing = false, error = e.message) }
                }
        }
    }

    fun toggleSave(id: Long) {
        viewModelScope.launch {
            repository.toggleSave(id).onFailure { e ->
                loadState.update { it.copy(error = e.message ?: "Could not update bookmark") }
            }
        }
    }
    fun markRead(id: Long) { viewModelScope.launch { repository.markRead(id) } }

    fun setFilter(tags: Set<String>) { categoryFilters.value = tags }
    fun clearFilter() { categoryFilters.value = emptySet() }

    fun setBlogFilter(blogId: Long?) {
        if (this.blogFilter.value != blogId) {
            this.blogFilter.value = blogId
            resetAndRefresh()
        }
    }

    fun clearBlogFilter() {
        if (this.blogFilter.value != null) {
            this.blogFilter.value = null
            resetAndRefresh()
        }
    }

    fun toggleSort() {
        sortOrder.update { if (it == SortOrder.NEWEST) SortOrder.TRENDING else SortOrder.NEWEST }
        resetAndRefresh()
    }

    fun clearError() { loadState.update { it.copy(error = null) } }
}
