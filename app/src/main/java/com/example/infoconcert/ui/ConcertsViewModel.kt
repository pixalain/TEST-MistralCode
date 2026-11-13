package com.example.infoconcert.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.infoconcert.data.Concert
import com.example.infoconcert.data.Department
import com.example.infoconcert.data.Departments
import com.example.infoconcert.data.InfoConcertRepository
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

class ConcertsViewModel : ViewModel() {

    private val repository = InfoConcertRepository()

    private val _uiState = MutableStateFlow(ConcertsUiState())
    val uiState: StateFlow<ConcertsUiState> = _uiState.asStateFlow()

    fun selectDepartment(department: Department) {
        _uiState.update { ConcertsUiState(department = department, isLoading = true) }
        viewModelScope.launch {
            try {
                val concerts = repository.loadConcerts(department)
                _uiState.update { it.copy(concerts = concerts, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Impossible de charger les concerts : ${e.message}")
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
            it.code.lowercase().contains(normalized) || it.name.lowercase().foldAccents().contains(normalized)
        }
    }

    private fun String.foldAccents(): String =
        java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace("'", " ")
}
