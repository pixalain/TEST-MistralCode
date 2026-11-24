package com.tvlive.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvlive.app.model.Channel
import com.tvlive.app.m3u.M3uParser
import com.tvlive.app.sources.SourceEntry
import com.tvlive.app.sources.SourcesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Élément de la liste des chaînes : en-tête de groupe ou chaîne. */
sealed interface ListItem {
    data class Header(val group: String, val count: Int) : ListItem
    data class Item(val channel: Channel) : ListItem
}

class ChannelsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SourcesRepository(app)

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    private val _query = MutableStateFlow("")

    val query: StateFlow<String> = _query.asStateFlow()
    val status = MutableStateFlow("")
    val busy = MutableStateFlow(false)

    val builtinPlaylists: List<Channel> = repository.builtinPlaylists()
    val savedSources: List<SourceEntry> get() = repository.savedSources()

    /** Liste aplatie avec en-têtes de groupes, filtrée par la recherche. */
    val items: StateFlow<List<ListItem>> = combine(_channels, _query) { channels, q ->
        buildList<ListItem> {
            for ((group, list) in M3uParser.group(M3uParser.search(channels, q))) {
                add(ListItem.Header(group, list.size))
                list.forEach { add(ListItem.Item(it)) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        addChannels(repository.builtinStreams())
        repository.savedSources().forEach { loadSource(it) }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun addPlaylistUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            busy.value = true
            try {
                val found = repository.fetchPlaylist(trimmed)
                addChannels(found)
                repository.addSource("url", trimmed, trimmed)
            } catch (e: Exception) {
                status.value = "Erreur : ${e.message}"
            } finally {
                busy.value = false
            }
        }
    }

    fun addLocalFile(uri: Uri, displayName: String) {
        viewModelScope.launch {
            busy.value = true
            try {
                val found = repository.readLocalPlaylist(uri)
                addChannels(found)
                repository.addSource("file", uri.toString(), displayName)
            } catch (e: Exception) {
                status.value = "Erreur : ${e.message}"
            } finally {
                busy.value = false
            }
        }
    }

    fun loadPlaylistEntry(channel: Channel) {
        if (channel.isPlaylist) addPlaylistUrl(channel.url)
    }

    fun loadSource(source: SourceEntry) {
        viewModelScope.launch {
            busy.value = true
            try {
                val found = if (source.kind == "file") {
                    repository.readLocalPlaylist(Uri.parse(source.path))
                } else {
                    repository.fetchPlaylist(source.path)
                }
                addChannels(found)
            } catch (e: Exception) {
                status.value = "Source ${source.name} indisponible : ${e.message}"
            } finally {
                busy.value = false
            }
        }
    }

    private fun addChannels(found: List<Channel>) {
        val known = _channels.value.map { it.url }.toHashSet()
        _channels.value = _channels.value + found.filter { it.url !in known }
        status.value = "${_channels.value.size} chaînes au total"
    }
}
