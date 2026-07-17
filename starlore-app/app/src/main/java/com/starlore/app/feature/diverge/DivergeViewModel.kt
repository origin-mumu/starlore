package com.starlore.app.feature.diverge

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starlore.app.data.api.AiApi
import com.starlore.app.data.api.DivergePair
import kotlinx.coroutines.launch

sealed interface DivergeUiState {
    object Idle : DivergeUiState
    object Loading : DivergeUiState
    data class Success(val pairs: List<DivergePair>) : DivergeUiState
    data class Error(val message: String) : DivergeUiState
}

class DivergeViewModel(private val aiApi: AiApi) : ViewModel() {

    var uiState = mutableStateOf<DivergeUiState>(DivergeUiState.Idle)
        private set

    var wordInput = mutableStateOf("")
    var coreWord = mutableStateOf("")

    val pairsList = mutableStateListOf<DivergePair>()

    fun startDivergence() {
        val word = wordInput.value.trim()
        if (word.isEmpty()) return

        uiState.value = DivergeUiState.Loading
        coreWord.value = word
        pairsList.clear()

        viewModelScope.launch {
            try {
                val response = aiApi.diverge(mapOf("word" to word))
                pairsList.addAll(response.pairs)
                uiState.value = DivergeUiState.Success(response.pairs)
            } catch (e: Exception) {
                uiState.value = DivergeUiState.Error(e.message ?: "Failed to brainstorm ideas")
            }
        }
    }
}
