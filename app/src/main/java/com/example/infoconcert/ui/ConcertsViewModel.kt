package com.example.infoconcert.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.infoconcert.data.Concert
import com.example.infoconcert.data.Department
import com.example.infoconcert.data.Departments
import com.example.infoconcert.data.WebViewConcertFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConcertsUiState(
    val department: Department? = null,
    val concerts: List<Concert> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ConcertsViewModel(application: Application) : AndroidViewModel(application) {

    private val fetcher = WebViewConcertFetcher()

    private val _uiState = MutableStateFlow(ConcertsUiState())
    val uiState: StateFlow<ConcertsUiState> = _uiState.asStateFlow()

    fun selectDepartment(department: Department) {
        _uiState.update {
            ConcertsUiState(department = department, isLoading = true)
        }
        load(department)
    }

    fun retry() {
        val department = _uiState.value.department ?: return
        _uiState.update {
            ConcertsUiState(department = department, isLoading = true)
        }
        load(department)
    }

    private fun load(department: Department) {
        viewModelScope.launch {
            try {
                val concerts = fetcher.fetch(getApplication(), department.url)
                _uiState.update { state ->
                    state.copy(
                        concerts = concerts,
                        isLoading = false,
                        error = if (concerts.isEmpty()) {
                            "Aucun concert détecté sur la page. La structure du site a peut-être changé."
                        } else {
                            null
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Impossible de charger les concerts : ${e.message ?: "erreur inconnue"}"
                    )
                }
            }
        }
    }

    fun backToDepartments() {
        _uiState.value = ConcertsUiState()
    }

    fun filteredDepartments(query: String): List<Department> {
        if (query.isBlank()) return Departments.all
        val normalized = query.lowercase().foldAccents()
        return Departments.all.filter {
            it.code.lowercase().contains(normalized) ||
                it.name.lowercase().foldAccents().contains(normalized)
        }
    }

    private fun String.foldAccents(): String =
        java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace("'", " ")
}
