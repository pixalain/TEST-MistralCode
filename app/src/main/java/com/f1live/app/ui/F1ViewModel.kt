package com.f1live.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    data class Success<T>(val data: T, val fromCache: Boolean = false) : UiState<T>
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

data class NewsUi(
    val lastRace: Race? = null,
    val lastNews: List<com.f1live.app.data.repository.NewsItem> = emptyList(),
    val nextRace: Race? = null,
    val nextNews: List<com.f1live.app.data.repository.NewsItem> = emptyList()
)

class F1ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: F1Repository = F1Repository(cache = com.f1live.app.data.repository.F1Cache(application))

    private val _standings = MutableStateFlow<UiState<StandingsUi>>(UiState.Loading)
    val standings: StateFlow<UiState<StandingsUi>> = _standings.asStateFlow()

    private val _races = MutableStateFlow<UiState<RacesUi>>(UiState.Loading)
    val races: StateFlow<UiState<RacesUi>> = _races.asStateFlow()

    private val _raceDetail = MutableStateFlow<UiState<RaceDetail>>(UiState.Loading)
    val raceDetail: StateFlow<UiState<RaceDetail>> = _raceDetail.asStateFlow()

    private val _driverProfile = MutableStateFlow<UiState<DriverProfile>>(UiState.Loading)
    val driverProfile: StateFlow<UiState<DriverProfile>> = _driverProfile.asStateFlow()

    private val _liveLaps = MutableStateFlow<UiState<com.f1live.app.data.repository.LiveLaps>>(UiState.Loading)
    val liveLaps: StateFlow<UiState<com.f1live.app.data.repository.LiveLaps>> = _liveLaps.asStateFlow()

    private val _news = MutableStateFlow<UiState<NewsUi>>(UiState.Loading)
    val news: StateFlow<UiState<NewsUi>> = _news.asStateFlow()

    fun loadNews() {
        _news.value = UiState.Loading
        viewModelScope.launch {
            try {
                val schedule = repository.seasonSchedule(_season.value)
                val races = schedule.data.second
                val now = System.currentTimeMillis()
                val withDates = races.mapNotNull { r ->
                    com.f1live.app.notifications.sessionEpochMillis(
                        com.f1live.app.data.model.Session(r.date, r.time)
                    )?.let { r to it }
                }
                val last = withDates.filter { it.second <= now }.maxByOrNull { it.second }?.first
                val next = withDates.filter { it.second > now }.minByOrNull { it.second }?.first
                val newsSerializer = kotlinx.serialization.serializer<List<com.f1live.app.data.repository.NewsItem>>()
                val cache = com.f1live.app.data.repository.F1Cache(getApplication())
                suspend fun newsFor(race: com.f1live.app.data.model.Race?): List<com.f1live.app.data.repository.NewsItem> {
                    if (race == null) return emptyList()
                    val key = "news_v2_" + race.round
                    cache.load(key)?.let { (raw, _) ->
                        val cached = com.f1live.app.data.repository.F1Json.json.decodeFromString(newsSerializer, raw)
                        if (cached.isNotEmpty()) return cached
                    }
                    val fresh = com.f1live.app.data.repository.fetchNewsForRace(race, 10)
                    if (fresh.isNotEmpty()) {
                        cache.save(key, com.f1live.app.data.repository.F1Json.json.encodeToString(newsSerializer, fresh))
                    }
                    return fresh
                }
                _news.value = UiState.Success(NewsUi(last, newsFor(last), next, newsFor(next)))
            } catch (e: Exception) {
                _news.value = UiState.Error(e.message ?: "Erreur actus")
            }
        }
    }

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated.asStateFlow()

    private val _season = MutableStateFlow("current")
    val season: StateFlow<String> = _season.asStateFlow()

    val availableSeasons: List<String> =
        (java.time.Year.now().value downTo 1950).map { it.toString() }

    init {
        refreshStandings()
        refreshRaces()
    }

    fun selectSeason(season: String) {
        if (season == _season.value) return
        _season.value = season
        refreshStandings()
        refreshRaces()
    }

    fun refreshStandings() {
        _standings.value = UiState.Loading
        viewModelScope.launch {
            try {
                val season = _season.value
                val driversDeferred = async { repository.driverStandings(season) }
                val constructorsDeferred = async { repository.constructorStandings(season) }
                val drivers = driversDeferred.await()
                val constructors = constructorsDeferred.await()
                val standingsSeason = drivers.data.first.ifBlank { constructors.data.first }
                val fromCache = drivers.fromCache || constructors.fromCache
                _standings.value = UiState.Success(
                    StandingsUi(
                        season = standingsSeason,
                        drivers = drivers.data.second,
                        constructors = constructors.data.second
                    ),
                    fromCache = fromCache
                )
                _lastUpdated.value = if (fromCache) {
                    maxOf(drivers.cachedAt, constructors.cachedAt)
                } else System.currentTimeMillis()
            } catch (e: Exception) {
                _standings.value = UiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun refreshRaces() {
        _races.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.seasonSchedule(_season.value)
                if (_season.value == "current" && result.data.first.isNotBlank()) {
                    _season.value = result.data.first
                }
                val (season, races) = result.data
                _races.value = UiState.Success(RacesUi(season, races), fromCache = result.fromCache)
                _lastUpdated.value = if (result.fromCache) result.cachedAt else System.currentTimeMillis()
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

    fun loadLiveLaps(round: String, season: String) {
        viewModelScope.launch {
            try {
                _liveLaps.value = UiState.Success(repository.liveLaps(round, season))
            } catch (e: Exception) {
                _liveLaps.value = UiState.Error(e.message ?: "Erreur live")
            }
        }
    }

    fun loadRace(round: String, season: String = _season.value) {
        _raceDetail.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.raceResults(round, season)
                _raceDetail.value = UiState.Success(result.data, fromCache = result.fromCache)
                _lastUpdated.value = if (result.fromCache) result.cachedAt else System.currentTimeMillis()
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
