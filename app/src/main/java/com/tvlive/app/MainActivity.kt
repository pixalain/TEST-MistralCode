@file:OptIn(UnstableApi::class)

package com.tvlive.app

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tvlive.app.ui.ChannelsScreen
import com.tvlive.app.ui.ChannelsViewModel
import com.tvlive.app.ui.DownloadsScreen
import com.tvlive.app.ui.PlayerScreen
import com.tvlive.app.ui.theme.TvLiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvLiveTheme {
                Surface {
                    TvLiveAppShell()
                }
            }
        }
    }
}

@Composable
private fun TvLiveAppShell() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: ChannelsViewModel = viewModel()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
            }
            val displayName = runCatching {
                context.contentResolver.query(
                    it,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null, null, null,
                )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull() ?: it.lastPathSegment ?: "playlist.m3u"
            viewModel.addLocalFile(it, displayName)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "channels",
                    onClick = {
                        navController.navigate("channels") {
                            popUpTo("channels")
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Tv, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_channels)) },
                )
                NavigationBarItem(
                    selected = currentRoute == "downloads",
                    onClick = {
                        navController.navigate("downloads") {
                            popUpTo("channels")
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_downloads)) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "channels",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("channels") {
                ChannelsScreen(
                    nav = navController,
                    viewModel = viewModel,
                    onPickFile = { filePicker.launch(arrayOf("*/*")) },
                )
            }
            composable("downloads") {
                DownloadsScreen(nav = navController)
            }
            composable("player/{url}/{name}") { entry ->
                val url = Uri.decode(entry.arguments?.getString("url") ?: "")
                val name = Uri.decode(entry.arguments?.getString("name") ?: "")
                PlayerScreen(nav = navController, url = url, name = name)
            }
        }
    }
}
