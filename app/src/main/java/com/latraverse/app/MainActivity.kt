package com.latraverse.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.latraverse.app.data.Concert
import com.latraverse.app.data.Concerts
import com.latraverse.app.data.Venue
import coil.compose.AsyncImage
import com.latraverse.app.ui.theme.LaTraverseTheme
import com.latraverse.app.ui.theme.TraverseYellow
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaTraverseTheme {
                LaTraverseApp()
            }
        }
    }
}

@Composable
fun LaTraverseApp() {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen { showSplash = false }
        return
    }

    LaTraverseMainContent()
}

@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = "https://www.latraverse.org/IMG/logo/logo_traverse_2025_noir.png?1756828759",
                contentDescription = "La Traverse",
                error = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier.size(220.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "TOUCHEZ POUR ENTRER",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaTraverseMainContent() {
    var selectedConcert by remember { mutableStateOf<Concert?>(null) }

    if (selectedConcert != null) {
        ConcertDetailScreen(selectedConcert!!) { selectedConcert = null }
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    val list = remember(query, tab) {
        val base = if (tab == 0) Concerts.upcoming() else Concerts.past()
        val q = query.trim().lowercase()
        if (q.isEmpty()) base else base.filter {
            it.artist.lowercase().contains(q) ||
                it.genre.lowercase().contains(q) ||
                (it.support?.lowercase()?.contains(q) ?: false)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "LA TRAVERSE — CLÉON",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(onClick = { tab = if (tab == 2) 0 else 2 }) {
                            Icon(Icons.Filled.Info, contentDescription = "Infos salle")
                        }
                    }
                )
                if (tab < 2) {
                    TabRow(selectedTabIndex = tab) {
                        Tab(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            text = { Text("À venir") },
                            icon = { Icon(Icons.Filled.Upcoming, contentDescription = null) }
                        )
                        Tab(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            text = { Text("Passés") },
                            icon = { Icon(Icons.Filled.History, contentDescription = null) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            tab == 2 -> VenueInfoScreen(Modifier.padding(padding))
            else -> Column(Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Rechercher un artiste ou un genre…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                if (list.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Aucun concert trouvé",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(list, key = { it.date.toString() + it.artist }) { concert ->
                            ConcertCard(concert) { selectedConcert = concert }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConcertCard(concert: Concert, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = concert.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = concert.date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                        .uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    concert.artist.uppercase(),
                    style = MaterialTheme.typography.titleMedium
                )
                if (concert.support != null) {
                    Text(
                        "avec ${concert.support}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    concert.genre.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                concert.time,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertDetailScreen(concert: Concert, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(concert.artist) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (concert.imageUrl != null) {
                AsyncImage(
                    model = concert.imageUrl,
                    contentDescription = concert.artist,
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
            Column(Modifier.padding(20.dp)) {
            Text(
                concert.formattedDate().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text("${concert.time} — ${Venue.NAME}, Cléon", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text(concert.genre, style = MaterialTheme.typography.titleMedium)
            if (concert.support != null) {
                Spacer(Modifier.height(6.dp))
                Text("Première partie : ${concert.support}")
            }
            if (concert.description != null) {
                Spacer(Modifier.height(12.dp))
                Text(concert.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            if (!concert.isPast) {
                ActionRow(Icons.Filled.Web, "Billetterie sur latraverse.org") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Venue.WEBSITE)))
                }
            }
            ActionRow(Icons.Filled.Map, "Itinéraire vers la salle") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Venue.MAP_URL)))
            }
            }
        }
    }
}

@Composable
fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TraverseYellow)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun VenueInfoScreen(modifier: Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(Venue.NAME.uppercase(), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Salle de concerts et de spectacles : blues, rock, soul, jazz, hip-hop, danse et théâtre. " +
                "Salle modulable de 250 à 650 places.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))
        ActionRow(Icons.Filled.Map, Venue.ADDRESS) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Venue.MAP_URL)))
        }
        ActionRow(Icons.Filled.Phone, Venue.PHONE) {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+33235812525")))
        }
        ActionRow(Icons.Filled.Web, "latraverse.org") {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Venue.WEBSITE)))
        }
    }
}
