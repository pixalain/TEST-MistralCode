@file:OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)

package com.tvlive.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.tvlive.app.R
import com.tvlive.app.model.Channel
import com.tvlive.app.player.FileDownloads
import com.tvlive.app.player.PlayerFactory
import com.tvlive.app.player.TvDownloads

/** Écran de lecture : vidéo ExoPlayer embarquée + actions télécharger / lecteur externe. */
@Composable
fun PlayerScreen(nav: NavController, url: String, name: String) {
    val context = LocalContext.current
    val exoPlayer = remember { PlayerFactory.create(context).apply { playWhenReady = true } }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = {
                    val channel = Channel(name, url)
                    if (channel.isPlaylist || url.lowercase().contains(".m3u8")) {
                        TvDownloads.enqueue(context, channel)
                        Toast.makeText(context, R.string.added_hls, Toast.LENGTH_LONG).show()
                    } else {
                        FileDownloads.enqueue(context, channel)
                        Toast.makeText(context, R.string.added_file, Toast.LENGTH_LONG).show()
                    }
                    nav.navigate("downloads") { launchSingleTop = true }
                }) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text(stringResource(R.string.download), modifier = Modifier.padding(start = 6.dp))
                }
                Button(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            type = if (url.lowercase().contains(".m3u8")) {
                                "application/x-mpegURL"
                            } else {
                                "video/*"
                            }
                        }
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.no_external_player, Toast.LENGTH_LONG).show()
                    }
                }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Text(stringResource(R.string.open_external), modifier = Modifier.padding(start = 6.dp))
                }
            }
            Text(
                text = stringResource(R.string.legal_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
