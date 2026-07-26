package dev.devscroll.ui.saved

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.devscroll.ui.PaginationConfig
import javax.inject.Inject

data class SavedUiState(
    val articles: List<Article> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
    val isBatchLimitReached: Boolean = false,
)

private data class SavedLoadState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
    // Kept in the flow rather than a plain field so isBatchLimitReached recomputes.
    val page: Int = 0,
)

@HiltViewModel
class SavedViewModel @Inject constructor(
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

    private val loadState = MutableStateFlow(SavedLoadState())

    val articles: StateFlow<List<Article>> = repository.observeSaved()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<SavedUiState> = combine(
        articles,
        loadState
    ) { articleList, loading ->
        SavedUiState(
            articles = articleList,
            isRefreshing = loading.isRefreshing,
            isLoadingMore = loading.isLoadingMore,
            hasNext = loading.hasNext,
            error = loading.error,
            isBatchLimitReached = (loading.page + 1) % PaginationConfig.MAX_PAGES_PER_BATCH == 0,
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedUiState(),
    )

    init {
        viewModelScope.launch {
            // userState re-emits on any profile write (streak, saved count, ...).
            // Only the transition into a signed-in session should trigger a refetch.
            userState
                .map { it != null }
                .distinctUntilChanged()
                .collect { isSignedIn -> if (isSignedIn) refresh() }
        }
    }

    fun refresh() {
        if (userState.value == null) return
        viewModelScope.launch {
            loadState.update { it.copy(isRefreshing = true, error = null, page = 0) }
            repository.clearSavedArticlesCache()
            repository.fetchSavedArticles(page = 0)
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    loadState.update { it.copy(isRefreshing = false, error = e.message) }
                }
        }
    }

    fun loadNextPage() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext || userState.value == null) return
        val limitReached = (state.page + 1) % PaginationConfig.MAX_PAGES_PER_BATCH == 0
        if (limitReached) return

        val nextPage = state.page + 1
        viewModelScope.launch {
            loadState.update { it.copy(isLoadingMore = true, page = nextPage) }
            repository.fetchSavedArticles(page = nextPage)
                .onSuccess { page ->
                    loadState.update { it.copy(isLoadingMore = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    loadState.update { it.copy(isLoadingMore = false, error = e.message, page = state.page) }
                }
        }
    }

    fun loadNextBatch() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext || userState.value == null) return
        val nextPage = state.page + 1
        viewModelScope.launch {
            loadState.update { it.copy(isRefreshing = true, error = null, page = nextPage) }
            repository.clearSavedArticlesCache()
            repository.fetchSavedArticles(page = nextPage)
                .onSuccess { page ->
                    loadState.update { it.copy(isRefreshing = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    loadState.update { it.copy(isRefreshing = false, error = e.message, page = state.page) }
                }
        }
    }

    fun resetAndRefresh() {
        if (userState.value == null) return
        viewModelScope.launch {
            loadState.update { it.copy(isRefreshing = true, error = null, page = 0) }
            repository.clearSavedArticlesCache()
            repository.fetchSavedArticles(page = 0)
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
}
