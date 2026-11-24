@file:OptIn(ExperimentalMaterial3Api::class)

package com.tvlive.app.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tvlive.app.R

@Composable
fun ChannelsScreen(
    nav: NavController,
    viewModel: ChannelsViewModel,
    onPickFile: () -> Unit,
) {
    val items by viewModel.items.collectAsState()
    val query by viewModel.query.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val status by viewModel.status.collectAsState()
    var showUrlDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    IconButton(onClick = { showUrlDialog = true }) {
                        Icon(
                            Icons.Default.AddLink,
                            contentDescription = stringResource(R.string.add_playlist_url),
                        )
                    }
                    IconButton(onClick = onPickFile) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.open_playlist_file),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            if (status.isNotEmpty()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_channels),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = items,
                    key = { item ->
                        when (item) {
                            is ListItem.Header -> "header:${item.group}"
                            is ListItem.Item -> "channel:${item.channel.url}"
                        }
                    },
                ) { item ->
                    when (item) {
                        is ListItem.Header -> Text(
                            text = "${item.group} (${item.count})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
                        )
                        is ListItem.Item -> {
                            val channel = item.channel
                            Text(
                                text = channel.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (channel.isPlaylist) {
                                            viewModel.loadPlaylistEntry(channel)
                                        } else {
                                            nav.navigate(
                                                "player/${Uri.encode(channel.url)}/${Uri.encode(channel.name)}"
                                            )
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUrlDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text(stringResource(R.string.playlist_url_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text(stringResource(R.string.playlist_url_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUrlDialog = false
                    viewModel.addPlaylistUrl(url)
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
