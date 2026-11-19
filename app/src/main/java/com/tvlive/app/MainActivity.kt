package com.tvlive.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
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

@OptIn(UnstableApi::class)
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

@OptIn(UnstableApi::class)
@Composable
private fun TvLiveAppShell() {
    val context = LocalContext.current
    val nav = rememberNavController()
    val vm: ChannelsViewModel = viewModel()

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
            val name = runCatching {
                context.contentResolver.query(it, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: it.lastPathSegment ?: "playlist.m3u"
            vm.addLocalFile(it, name)
        }
    }

    val navBackStack by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "channels",
                    onClick = {
                        nav.navigate("channels") {
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
                        nav.navigate("downloads") {
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
            navController = nav,
            startDestination = "channels",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("channels") {
                ChannelsScreen(
                    nav = nav,
                    vm = vm,
                    onPickFile = { filePicker.launch(arrayOf("*/*")) },
                )
            }
            composable("downloads") {
                DownloadsScreen(nav = nav)
            }
            composable("player/{url}/{name}") { backStackEntry ->
                val url = Uri.decode(backStackEntry.arguments?.getString("url") ?: "")
                val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
                PlayerScreen(nav = nav, url = url, name = name)
            }
        }
    }
}
