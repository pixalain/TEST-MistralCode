package com.tvlive.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvlive.app.data.model.Channel
import com.tvlive.app.data.m3u.M3uParser
import com.tvlive.app.data.sources.SourceEntry
import com.tvlive.app.data.sources.SourcesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ListItem {
    data class Header(val group: String, val count: Int) : ListItem
    data class Item(val channel: Channel) : ListItem
}

class ChannelsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SourcesRepository(app)

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val status = MutableStateFlow("")
    val busy = MutableStateFlow(false)

    val builtinPlaylists: List<Channel> = repo.builtinPlaylists()
    val savedSources: List<SourceEntry> get() = repo.savedSources()

    val items: StateFlow<List<ListItem>> = combine(_channels, _query) { chs, q ->
        buildList<ListItem> {
            for ((group, list) in M3uParser.group(M3uParser.search(chs, q))) {
                add(ListItem.Header(group, list.size))
                list.forEach { add(ListItem.Item(it)) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        addChannels(repo.builtinStreams())
        for (src in repo.savedSources()) {
            loadSource(src)
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun loadBuiltinPlaylist(channel: Channel) {
        if (channel.isPlaylist) addPlaylistUrl(channel.url)
    }

    fun addPlaylistUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            busy.value = true
            try {
                val found = repo.fetchPlaylist(url.trim())
                addChannels(found)
                repo.addSource("url", url.trim(), url.trim())
                status.value = "${found.size} chaînes ajoutées ($url)"
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
                val found = repo.readLocalFile(uri)
                addChannels(found)
                repo.addSource("file", uri.toString(), displayName)
                status.value = "${found.size} chaînes ajoutées ($displayName)"
            } catch (e: Exception) {
                status.value = "Erreur : ${e.message}"
            } finally {
                busy.value = false
            }
        }
    }

    fun loadSource(src: SourceEntry) {
        viewModelScope.launch {
            busy.value = true
            try {
                val found = if (src.kind == "file") {
                    repo.readLocalFile(Uri.parse(src.path))
                } else {
                    repo.fetchPlaylist(src.path)
                }
                addChannels(found)
            } catch (e: Exception) {
                status.value = "Source ${src.name} indisponible : ${e.message}"
            } finally {
                busy.value = false
            }
        }
    }

    fun removeSource(src: SourceEntry) {
        repo.removeSource(src.path)
    }

    private fun addChannels(found: List<Channel>) {
        val known = _channels.value.map { it.url }.toSet()
        _channels.value = _channels.value + found.filter { it.url !in known }
        status.value = "${_channels.value.size} chaînes au total"
    }
}
