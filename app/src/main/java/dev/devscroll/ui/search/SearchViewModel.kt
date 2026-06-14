package dev.devscroll.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devscroll.data.model.Article
import dev.devscroll.data.model.Source
import dev.devscroll.data.model.User
import dev.devscroll.data.repository.ArticleRepository
import dev.devscroll.data.repository.AuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val userState: StateFlow<User?> = authRepository.userState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val topTags: StateFlow<List<String>> = repository.observeArticles()
        .map { articles ->
            articles.flatMap { it.categories }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(24)
                .map { it.key }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sources: StateFlow<List<Source>> = repository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val results: StateFlow<List<Article>> = combine(
        repository.observeArticles(),
        _query,
    ) { articles, query ->
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            articles.filter { article ->
                article.title.lowercase().contains(q) ||
                article.description.lowercase().contains(q) ||
                article.blogName.lowercase().contains(q) ||
                article.categories.any { it.lowercase().contains(q) } ||
                article.authors.any { it.lowercase().contains(q) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun toggleSave(id: Long) {
        viewModelScope.launch { repository.toggleSave(id) }
    }

    fun markRead(id: Long) {
        viewModelScope.launch { repository.markRead(id) }
    }
}
