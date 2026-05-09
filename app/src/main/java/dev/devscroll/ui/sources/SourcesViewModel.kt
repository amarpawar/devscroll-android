package dev.devscroll.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.devscroll.data.model.Source
import dev.devscroll.data.repository.ArticleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(
    repository: ArticleRepository,
) : ViewModel() {

    val sources: StateFlow<List<Source>> = repository.observeSources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
