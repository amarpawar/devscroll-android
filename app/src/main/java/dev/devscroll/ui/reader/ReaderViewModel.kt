package dev.devscroll.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devscroll.data.model.Article
import dev.devscroll.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ArticleRepository,
) : ViewModel() {

    private val _article = MutableStateFlow<Article?>(null)
    val article: StateFlow<Article?> = _article.asStateFlow()

    fun loadArticle(id: Long) {
        viewModelScope.launch {
            _article.value = repository.getArticle(id)
            repository.markRead(id)
        }
    }

    fun toggleSave(id: Long) {
        viewModelScope.launch {
            repository.toggleSave(id)
            _article.value = repository.getArticle(id)
        }
    }
}
