package com.f1live.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.f1live.app.ui.F1ViewModel
import com.f1live.app.ui.screens.RaceDetailScreen
import com.f1live.app.ui.screens.DriverProfileScreen
import com.f1live.app.ui.screens.NewsScreen
import com.f1live.app.ui.screens.RacesScreen
import com.f1live.app.ui.screens.StandingsScreen
import com.f1live.app.ui.theme.F1LiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            F1LiveTheme {
                F1App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun F1App(viewModel: F1ViewModel = viewModel()) {
    val navController = rememberNavController()
    val standingsState by viewModel.standings.collectAsStateWithLifecycle()
    val racesState by viewModel.races.collectAsStateWithLifecycle()
    val detailState by viewModel.raceDetail.collectAsStateWithLifecycle()
    val driverState by viewModel.driverProfile.collectAsStateWithLifecycle()
    val driverPhotoPath by viewModel.driverPhotoPath.collectAsStateWithLifecycle()
    val liveState by viewModel.liveLaps.collectAsStateWithLifecycle()
    val newsState by viewModel.news.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // (Re)schedule session reminders whenever the schedule is (re)loaded
    LaunchedEffect(racesState) {
        val s = racesState
        if (s is com.f1live.app.ui.UiState.Success) {
            com.f1live.app.notifications.scheduleSessionReminders(context, s.data.races)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val drawerState = androidx.compose.material3.rememberDrawerState(
        androidx.compose.material3.DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()
    val season by viewModel.season.collectAsStateWithLifecycle()

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet {
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = stringResource(R.string.menu_season),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(viewModel.availableSeasons) { s ->
                            androidx.compose.material3.NavigationDrawerItem(
                                label = { Text(s) },
                                selected = s == season,
                                onClick = {
                                    viewModel.selectSeason(s)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.menu_open)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        when (currentRoute) {
                            "standings" -> viewModel.refreshStandings()
                            "races" -> viewModel.refreshRaces()
                            "news" -> viewModel.loadNews()
                            else -> {}
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "standings",
                    onClick = { navController.navigate("standings") { popUpTo(0) } },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_standings)) }
                )
                NavigationBarItem(
                    selected = currentRoute == "races",
                    onClick = { navController.navigate("races") { popUpTo(0) } },
                    icon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_races)) }
                )
                NavigationBarItem(
                    selected = currentRoute == "news",
                    onClick = { navController.navigate("news") { popUpTo(0) } },
                    icon = { Icon(Icons.Default.Article, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_news)) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "standings",
            modifier = Modifier.padding(padding)
        ) {
            composable("standings") {
                StandingsScreen(
                    state = standingsState,
                    onRefresh = { viewModel.refreshStandings() },
                    onDriverClick = { driverId, team ->
                        navController.navigate("driver/$driverId?team=$team")
                    },
                    lastUpdated = lastUpdated
                )
            }
            composable(
                route = "driver/{driverId}?team={team}",
                arguments = listOf(
                    navArgument("driverId") { type = NavType.StringType },
                    navArgument("team") { type = NavType.StringType; defaultValue = ""; nullable = true }
                )
            ) { entry ->
                val driverId = entry.arguments?.getString("driverId") ?: ""
                val team = entry.arguments?.getString("team")?.ifBlank { null }
                LaunchedEffect(driverId) { viewModel.loadDriver(driverId, team) }
                DriverProfileScreen(
                    state = driverState,
                    onBack = { navController.popBackStack() },
                    onRefresh = { viewModel.loadDriver(driverId, team) },
                    photoPath = driverPhotoPath
                )
            }
            composable("races") {
                RacesScreen(
                    state = racesState,
                    onRefresh = { viewModel.refreshRaces() },
                    onRaceClick = { race ->
                        val season = (racesState as? com.f1live.app.ui.UiState.Success)?.data?.season ?: "current"
                        navController.navigate("race/${race.round}/?season=$season")
                    },
                    lastUpdated = lastUpdated
                )
            }
            composable("news") {
                LaunchedEffect(Unit) { viewModel.loadNews() }
                NewsScreen(
                    state = newsState,
                    onRefresh = { viewModel.loadNews() }
                )
            }
            composable(
                route = "race/{round}/{tab}?season={season}",
                arguments = listOf(
                    navArgument("round") { type = NavType.StringType },
                    navArgument("tab") { type = NavType.StringType; defaultValue = "" },
                    navArgument("season") { type = NavType.StringType; defaultValue = "current" }
                )
            ) { entry ->
                val round = entry.arguments?.getString("round") ?: "1"
                val tab = entry.arguments?.getString("tab").orEmpty()
                val season = entry.arguments?.getString("season") ?: "current"
                LaunchedEffect(round, season) { viewModel.loadRace(round, season) }
                RaceDetailScreen(
                    round = round,
                    initialTab = tab,
                    state = detailState,
                    liveState = liveState,
                    onBack = { navController.popBackStack() },
                    onRefresh = { viewModel.loadRace(round, season) },
                    onRefreshLive = { viewModel.loadLiveLaps(round, detailState.let { (it as? com.f1live.app.ui.UiState.Success)?.data?.race?.season ?: season }) }
                )
            }
        }
    }
    }
}
