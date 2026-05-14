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
)

private data class LoadState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: ArticleRepository,
) : ViewModel() {

    private val categoryFilters = MutableStateFlow<Set<String>>(emptySet())
    private val sortOrder = MutableStateFlow(SortOrder.NEWEST)
    private val loadState = MutableStateFlow(LoadState())
    private var currentPage = 0

    private val _newArticleCount = MutableStateFlow(0)
    val newArticleCount: StateFlow<Int> = _newArticleCount.asStateFlow()

    val uiState: StateFlow<FeedUiState> = combine(
        repository.observeArticles(),
        categoryFilters,
        sortOrder,
        loadState,
    ) { articles, filters, sort, load ->
        val filtered = articles
            .filter { filters.isEmpty() || it.categories.any { cat -> cat in filters } }
            .let { list ->
                when (sort) {
                    SortOrder.NEWEST -> list.sortedByDescending { it.publishedOn }
                    SortOrder.HOT -> list.sortedByDescending { it.hitCount }
                }
            }
        FeedUiState(
            articles = filtered,
            isRefreshing = load.isRefreshing,
            isLoadingMore = load.isLoadingMore,
            hasNext = load.hasNext,
            error = load.error,
            activeCategoryFilters = filters,
            sortOrder = sort,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            currentPage = 0
            val existingIds = repository.getArticleIds()
            loadState.update { it.copy(isRefreshing = true, error = null) }
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

    fun clearNewArticleCount() { _newArticleCount.value = 0 }

    fun loadNextPage() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext) return
        viewModelScope.launch {
            currentPage++
            loadState.update { it.copy(isLoadingMore = true) }
            repository.fetchArticles(page = currentPage)
                .onSuccess { page ->
                    loadState.update { it.copy(isLoadingMore = false, hasNext = page.hasNext) }
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
