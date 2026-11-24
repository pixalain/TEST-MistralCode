@file:OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)

package com.tvlive.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.tvlive.app.R
import com.tvlive.app.player.FileDownloads
import com.tvlive.app.player.TvDownloads
import java.io.File
import kotlinx.coroutines.delay

/** Onglet Téléchargements : replays HLS hors ligne (Media3) + fichiers téléchargés. */
@Composable
fun DownloadsScreen(nav: NavController) {
    val context = LocalContext.current
    var hlsDownloads by remember { mutableStateOf<List<Download>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            if (TvDownloads.isInitialized) hlsDownloads = TvDownloads.currentDownloads()
            delay(500)
        }
    }
    val fileJobs by FileDownloads.jobs.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_downloads)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.hls_section),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (hlsDownloads.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_downloads),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(hlsDownloads, key = { it.request.id }) { download ->
                val name = download.request.data?.toString(Charsets.UTF_8)
                    ?.ifEmpty { download.request.id }
                    ?: download.request.id
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = hlsStateLabel(download.state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val percent = download.percentDownloaded
                    if (percent.isNaN()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            nav.navigate(
                                "player/${Uri.encode(download.request.uri.toString())}/${Uri.encode(name)}"
                            )
                        }) { Text(stringResource(R.string.play)) }
                        OutlinedButton(onClick = { TvDownloads.remove(context, download.request.id) }) {
                            Text(stringResource(R.string.remove))
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.files_section),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(fileJobs, key = { it.id }) { job ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = job.name, style = MaterialTheme.typography.bodyLarge)
                    when (job.status) {
                        "running" -> {
                            Text(
                                text = "${(job.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LinearProgressIndicator(
                                progress = { job.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        "done" -> Text(
                            text = "Terminé",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        else -> Text(
                            text = job.error ?: "Échec",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (job.status == "done" && job.path != null) {
                            Button(onClick = {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "com.tvlive.app.fileprovider",
                                    File(job.path),
                                )
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                )
                            }) { Text(stringResource(R.string.open_file)) }
                        }
                        OutlinedButton(onClick = { FileDownloads.delete(job) }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

private fun hlsStateLabel(state: Int): String = when (state) {
    Download.STATE_QUEUED -> "En attente"
    Download.STATE_DOWNLOADING -> "En cours"
    Download.STATE_STOPPED -> "En pause"
    Download.STATE_COMPLETED -> "Terminé"
    Download.STATE_FAILED -> "Échec"
    Download.STATE_REMOVING -> "Suppression…"
    Download.STATE_RESTARTING -> "Redémarrage…"
    else -> "$state"
}
