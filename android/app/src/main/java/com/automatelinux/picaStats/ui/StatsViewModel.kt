package com.automatelinux.picaStats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automatelinux.picaStats.data.StatsRepository
import com.automatelinux.picaStats.data.StatsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repo: StatsRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val data: StatsResponse) : UiState
        data class Error(val message: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            if (_state.value !is UiState.Success) _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repo.fetch())
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Failed to load stats")
            }
            _refreshing.value = false
        }
    }
}
