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

enum class SourceSort {
    DEFAULT, NAME, POSTS
}

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

    private val _sortType = MutableStateFlow(SourceSort.DEFAULT)
    val sortType: StateFlow<SourceSort> = _sortType

    val sources: StateFlow<List<Source>> = combine(
        repository.observeSources(),
        _sortType
    ) { list, sort ->
        when (sort) {
            SourceSort.DEFAULT -> list
            SourceSort.NAME -> list.sortedBy { it.blogName.lowercase() }
            SourceSort.POSTS -> list.sortedByDescending { it.articleCount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun toggleSort() {
        _sortType.value = when (_sortType.value) {
            SourceSort.DEFAULT -> SourceSort.NAME
            SourceSort.NAME -> SourceSort.POSTS
            SourceSort.POSTS -> SourceSort.DEFAULT
        }
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
