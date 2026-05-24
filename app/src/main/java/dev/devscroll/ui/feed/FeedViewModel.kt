package dev.devscroll.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devscroll.data.model.Article
import dev.devscroll.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder { NEWEST, HOT }

data class FeedUiState(
    val articles: List<Article> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
    val activeCategoryFilters: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val pagesTrimmed: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
)

private data class LoadState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
    val pagesTrimmed: Boolean = false,
    val searchQuery: String? = null,
)

private const val PAGE_SIZE = 20
private const val MAX_PAGES = 10

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: ArticleRepository,
) : ViewModel() {

    private val categoryFilters = MutableStateFlow<Set<String>>(emptySet())
    private val sortOrder = MutableStateFlow(SortOrder.NEWEST)
    private val loadState = MutableStateFlow(LoadState())
    private var currentPage = 0
    private var firstLoadedPage = 0

    private val _newArticleCount = MutableStateFlow(0)
    val newArticleCount: StateFlow<Int> = _newArticleCount.asStateFlow()

    private val articleSource = loadState
        .map { it.searchQuery }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query == null) repository.observeArticles()
            else repository.observeSearchResults()
        }

    val uiState: StateFlow<FeedUiState> = combine(
        articleSource,
        categoryFilters,
        sortOrder,
        loadState,
    ) { articles, filters, sort, load ->
        val isSearchActive = load.searchQuery != null
        val processed = if (isSearchActive) {
            articles
        } else {
            articles
                .filter { filters.isEmpty() || it.categories.any { cat -> cat in filters } }
                .let { list ->
                    when (sort) {
                        SortOrder.NEWEST -> list.sortedByDescending { it.publishedOn }
                        SortOrder.HOT -> list.sortedByDescending { it.hitCount }
                    }
                }
        }
        FeedUiState(
            articles = processed,
            isRefreshing = load.isRefreshing,
            isLoadingMore = load.isLoadingMore,
            hasNext = load.hasNext,
            error = load.error,
            activeCategoryFilters = filters,
            sortOrder = sort,
            pagesTrimmed = load.pagesTrimmed,
            isSearchActive = isSearchActive,
            searchQuery = load.searchQuery ?: "",
        )
    }
        .flowOn(Dispatchers.Default)
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
            firstLoadedPage = 0
            val existingIds = repository.getArticleIds()
            loadState.update { it.copy(isRefreshing = true, error = null, pagesTrimmed = false) }
            repository.fetchArticles(page = 0)
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                    val newCount = repository.getArticleIds().count { it !in existingIds }
                    if (newCount > 0) _newArticleCount.value = newCount
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

    fun reloadEvictedPages() {
        val pagesToReload = firstLoadedPage
        if (pagesToReload == 0) return
        viewModelScope.launch {
            loadState.update { it.copy(isRefreshing = true, error = null, pagesTrimmed = false) }
            for (page in 0 until pagesToReload) {
                repository.fetchArticles(page = page)
                    .onFailure { e ->
                        loadState.update { it.copy(isRefreshing = false, error = e.message) }
                        return@launch
                    }
            }
            firstLoadedPage = 0
            loadState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearNewArticleCount() { _newArticleCount.value = 0 }

    fun loadNextPage() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext) return
        viewModelScope.launch {
            currentPage++
            loadState.update { it.copy(isLoadingMore = true) }
            val result = if (state.searchQuery != null) {
                repository.search(query = state.searchQuery, page = currentPage)
            } else {
                repository.fetchArticles(page = currentPage)
            }
            result
                .onSuccess { page ->
                    val trimmed = state.searchQuery == null && currentPage >= MAX_PAGES
                    if (trimmed) {
                        repository.deleteNewestArticles(PAGE_SIZE)
                        firstLoadedPage++
                    }
                    loadState.update { it.copy(
                        isLoadingMore = false,
                        hasNext = page.hasNext,
                        pagesTrimmed = if (state.searchQuery == null) firstLoadedPage > 0 else false,
                    )}
                }
                .onFailure { e ->
                    currentPage--
                    loadState.update { it.copy(isLoadingMore = false, error = e.message) }
                }
        }
    }

    fun toggleSave(id: Long) { viewModelScope.launch { repository.toggleSave(id) } }
    fun markRead(id: Long) { viewModelScope.launch { repository.markRead(id) } }

    fun setFilter(tags: Set<String>) { categoryFilters.value = tags }
    fun clearFilter() { categoryFilters.value = emptySet() }

    fun toggleSort() {
        sortOrder.update { if (it == SortOrder.NEWEST) SortOrder.HOT else SortOrder.NEWEST }
    }

    fun clearError() { loadState.update { it.copy(error = null) } }
}
