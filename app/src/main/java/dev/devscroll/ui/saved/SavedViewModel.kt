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

    private val loadState = MutableStateFlow(SavedUiState())
    private var currentPage = 0

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
        val limitReached = (currentPage + 1) % PaginationConfig.MAX_PAGES_PER_BATCH == 0
        loading.copy(articles = articleList, isBatchLimitReached = limitReached)
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedUiState(),
    )

    init {
        viewModelScope.launch {
            userState.collect { user ->
                if (user != null) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        if (userState.value == null) return
        viewModelScope.launch {
            currentPage = 0
            loadState.update { it.copy(isRefreshing = true, error = null) }
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
        val limitReached = (currentPage + 1) % PaginationConfig.MAX_PAGES_PER_BATCH == 0
        if (limitReached) return

        viewModelScope.launch {
            currentPage++
            loadState.update { it.copy(isLoadingMore = true) }
            repository.fetchSavedArticles(page = currentPage)
                .onSuccess { page ->
                    loadState.update { it.copy(isLoadingMore = false, hasNext = page.hasNext) }
                }
                .onFailure { e ->
                    currentPage--
                    loadState.update { it.copy(isLoadingMore = false, error = e.message) }
                }
        }
    }

    fun loadNextBatch() {
        val state = loadState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasNext || userState.value == null) return
        viewModelScope.launch {
            currentPage++
            loadState.update { it.copy(isRefreshing = true, error = null) }
            repository.clearSavedArticlesCache()
            repository.fetchSavedArticles(page = currentPage)
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
        if (userState.value == null) return
        viewModelScope.launch {
            currentPage = 0
            loadState.update { it.copy(isRefreshing = true, error = null) }
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
        viewModelScope.launch { repository.toggleSave(id) }
    }
}
