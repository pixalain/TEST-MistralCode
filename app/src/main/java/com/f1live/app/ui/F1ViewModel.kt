package com.f1live.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f1live.app.data.model.ConstructorStanding
import com.f1live.app.data.model.DriverStanding
import com.f1live.app.data.model.Race
import com.f1live.app.data.model.Result
import com.f1live.app.data.repository.DriverProfile
import com.f1live.app.data.repository.F1Repository
import com.f1live.app.data.repository.RaceDetail
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class StandingsUi(
    val season: String = "",
    val drivers: List<DriverStanding> = emptyList(),
    val constructors: List<ConstructorStanding> = emptyList()
)

data class RacesUi(
    val season: String = "",
    val races: List<Race> = emptyList()
)

class F1ViewModel(
    private val repository: F1Repository = F1Repository()
) : ViewModel() {

    private val _standings = MutableStateFlow<UiState<StandingsUi>>(UiState.Loading)
    val standings: StateFlow<UiState<StandingsUi>> = _standings.asStateFlow()

    private val _races = MutableStateFlow<UiState<RacesUi>>(UiState.Loading)
    val races: StateFlow<UiState<RacesUi>> = _races.asStateFlow()

    private val _raceDetail = MutableStateFlow<UiState<RaceDetail>>(UiState.Loading)
    val raceDetail: StateFlow<UiState<RaceDetail>> = _raceDetail.asStateFlow()

    private val _driverProfile = MutableStateFlow<UiState<DriverProfile>>(UiState.Loading)
    val driverProfile: StateFlow<UiState<DriverProfile>> = _driverProfile.asStateFlow()

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated.asStateFlow()

    init {
        refreshStandings()
        refreshRaces()
    }

    fun refreshStandings() {
        _standings.value = UiState.Loading
        viewModelScope.launch {
            try {
                val driversDeferred = async { repository.driverStandings() }
                val constructorsDeferred = async { repository.constructorStandings() }
                val drivers = driversDeferred.await()
                val constructors = constructorsDeferred.await()
                val season = drivers.first.ifBlank { constructors.first }
                _standings.value = UiState.Success(
                    StandingsUi(
                        season = season,
                        drivers = drivers.second,
                        constructors = constructors.second
                    )
                )
                _lastUpdated.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _standings.value = UiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun refreshRaces() {
        _races.value = UiState.Loading
        viewModelScope.launch {
            try {
                val (season, races) = repository.seasonSchedule()
                _races.value = UiState.Success(RacesUi(season, races))
                _lastUpdated.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _races.value = UiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun loadDriver(driverId: String, currentTeam: String? = null) {
        _driverProfile.value = UiState.Loading
        viewModelScope.launch {
            try {
                _driverProfile.value = UiState.Success(repository.driverProfile(driverId, currentTeam))
                _lastUpdated.value = System.currentTimeMillis()
            } catch (e: Exception) {
                val msg = when {
                    e is retrofit2.HttpException && e.code() == 429 ->
                        "Trop de requêtes vers l'API F1, réessayez dans quelques instants."
                    e is java.net.UnknownHostException || e is java.net.SocketTimeoutException ->
                        "Connexion impossible — vérifiez votre réseau."
                    else -> e.message ?: "Erreur inconnue"
                }
                _driverProfile.value = UiState.Error(msg)
            }
        }
    }

    fun loadRace(round: String) {
        _raceDetail.value = UiState.Loading
        viewModelScope.launch {
            try {
                _raceDetail.value = UiState.Success(repository.raceResults(round))
                _lastUpdated.value = System.currentTimeMillis()
            } catch (e: Exception) {
                val msg = when {
                    e is retrofit2.HttpException && e.code() == 429 ->
                        "Trop de requêtes vers l'API F1, réessayez dans quelques instants."
                    e is java.net.UnknownHostException || e is java.net.SocketTimeoutException ->
                        "Connexion impossible — vérifiez votre réseau."
                    else -> e.message ?: "Erreur inconnue"
                }
                _raceDetail.value = UiState.Error(msg)
            }
        }
    }
}
