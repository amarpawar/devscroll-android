package dev.devscroll.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devscroll.data.model.Article
import dev.devscroll.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: ArticleRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

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
}
