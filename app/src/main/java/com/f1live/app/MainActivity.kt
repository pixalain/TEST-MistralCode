package com.f1live.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
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
import com.f1live.app.ui.screens.RacesScreen
import com.f1live.app.ui.screens.SessionsScreen
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

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = {
                        when (currentRoute) {
                            "standings" -> viewModel.refreshStandings()
                            "races", "sessions" -> viewModel.refreshRaces()
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
                    selected = currentRoute == "sessions",
                    onClick = { navController.navigate("sessions") { popUpTo(0) } },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_sessions)) }
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
                    }
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
                    onRefresh = { viewModel.loadDriver(driverId, team) }
                )
            }
            composable("races") {
                RacesScreen(
                    state = racesState,
                    onRefresh = { viewModel.refreshRaces() },
                    onRaceClick = { race ->
                        navController.navigate("race/${race.round}/")
                    }
                )
            }
            composable("sessions") {
                SessionsScreen(
                    state = racesState,
                    onRefresh = { viewModel.refreshRaces() },
                    onRaceClick = { race, tab ->
                        navController.navigate("race/${race.round}/$tab")
                    }
                )
            }
            composable(
                route = "race/{round}/{tab}",
                arguments = listOf(
                    navArgument("round") { type = NavType.StringType },
                    navArgument("tab") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val round = entry.arguments?.getString("round") ?: "1"
                val tab = entry.arguments?.getString("tab").orEmpty()
                LaunchedEffect(round) { viewModel.loadRace(round) }
                RaceDetailScreen(
                    round = round,
                    initialTab = tab,
                    state = detailState,
                    onBack = { navController.popBackStack() },
                    onRefresh = { viewModel.loadRace(round) }
                )
            }
        }
    }
}
