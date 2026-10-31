package dev.devscroll.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devscroll.data.model.Source
import dev.devscroll.data.model.User
import dev.devscroll.data.repository.ArticleRepository
import dev.devscroll.data.repository.AuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refreshSources()
    }

    fun refreshSources() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshSources()
            _isRefreshing.value = false
        }
    }

    val userState: StateFlow<User?> = authRepository.userState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val sources: StateFlow<List<Source>> = repository.observeSources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** [sources] narrowed by [query]; matches on blog name or url. */
    val visibleSources: StateFlow<List<Source>> = combine(sources, _query) { list, query ->
        val q = query.trim()
        if (q.isEmpty()) list
        else list.filter { it.blogName.contains(q, ignoreCase = true) || it.url.contains(q, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun toggleSearch() {
        if (_isSearchOpen.value) closeSearch() else _isSearchOpen.value = true
    }

    fun closeSearch() {
        _isSearchOpen.value = false
        _query.value = ""
    }

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun toggleSubscribe(blogId: Long) {
        val source = sources.value.find { it.blogId == blogId } ?: return
        viewModelScope.launch {
            if (source.isSubscribed) {
                repository.unsubscribeBlog(blogId)
            } else {
                repository.subscribeBlog(blogId)
            }
        }
    }
}
