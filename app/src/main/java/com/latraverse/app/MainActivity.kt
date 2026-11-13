package com.latraverse.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.latraverse.app.data.ArchiveRepository
import com.latraverse.app.data.Concert
import com.latraverse.app.data.ConcertRepository
import com.latraverse.app.data.Concerts
import com.latraverse.app.data.RemoteSource
import com.latraverse.app.data.TremplinEdition
import com.latraverse.app.data.TremplinRepository
import com.latraverse.app.data.Venue
import coil.compose.AsyncImage
import com.latraverse.app.ui.theme.LaTraverseTheme
import com.latraverse.app.ui.theme.TraverseYellow
import kotlinx.coroutines.launch
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
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(
                        if (dark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://www.latraverse.org/IMG/logo/logo_traverse_2025_noir.png?1756828759",
                    contentDescription = "La Traverse",
                    error = painterResource(R.mipmap.ic_launcher_foreground),
                    modifier = Modifier.size(200.dp)
                )
            }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var base by remember { mutableStateOf(Concerts.all) }
    var concerts by remember { mutableStateOf(Concerts.all) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var lastUpdate by remember { mutableStateOf<String?>(null) }

    suspend fun loadRemote() {
        val remote = RemoteSource.fetchUpcoming()
        if (remote.isNotEmpty()) {
            concerts = ConcertRepository.merge(base, remote)
            lastUpdate = ConcertRepository.nowLabel()
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) {
        base = ConcertRepository.withArchive(Concerts.all, ArchiveRepository.load(context))
        concerts = base
        loadRemote()
    }

    val list = remember(query, tab, concerts) {
        val base = if (tab == 0) ConcertRepository.upcoming(concerts) else ConcertRepository.past(concerts)
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
                        IconButton(onClick = { tab = if (tab == 3) 0 else 3 }) {
                            Icon(Icons.Filled.Info, contentDescription = "Infos salle")
                        }
                    }
                )
                if (tab < 3) {
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
                        Tab(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            text = { Text("Tremplin") },
                            icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            tab == 3 -> VenueInfoScreen(Modifier.padding(padding))
            tab == 2 -> TremplinScreen(Modifier.padding(padding))
            else -> Column(Modifier.padding(padding).fillMaxSize()) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(28.dp),
                        strokeWidth = 3.dp,
                        color = TraverseYellow
                    )
                } else if (tab == 0 && lastUpdate != null) {
                    Text(
                        "Programmation mise à jour à $lastUpdate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
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
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        refreshing = true
                        scope.launch { loadRemote() }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
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
}

@Composable
fun ConcertCard(concert: Concert, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
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
    var details by remember { mutableStateOf<RemoteSource.EventDetails?>(null) }

    LaunchedEffect(concert.eventUrl) {
        if (concert.eventUrl != null) {
            details = RemoteSource.fetchEventDetails(concert.eventUrl)
        }
    }

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
                Box {
                    AsyncImage(
                        model = concert.imageUrl,
                        contentDescription = concert.artist,
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.mipmap.ic_launcher_foreground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                    if (details?.photoCredit != null) {
                        Text(
                            details!!.photoCredit!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
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
            val fullDescription = details?.description ?: concert.description
            if (fullDescription != null) {
                Spacer(Modifier.height(12.dp))
                Text(fullDescription, style = MaterialTheme.typography.bodyMedium)
            }
            if (details?.youtubeId != null) {
                Spacer(Modifier.height(8.dp))
                ActionRow(Icons.Filled.PlayArrow, "Voir la vidéo de l'artiste") {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/watch?v=${details!!.youtubeId}")
                        )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (!concert.isPast) {
                ActionRow(Icons.Filled.Web, "Réserver un billet") {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(concert.ticketUrl ?: Venue.WEBSITE))
                    )
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
fun TremplinScreen(modifier: Modifier) {
    var editions by remember { mutableStateOf(TremplinRepository.bundled) }
    var selected by remember { mutableStateOf<TremplinEdition?>(null) }

    LaunchedEffect(Unit) {
        val remote = TremplinRepository.fetchEditions()
        if (remote.isNotEmpty()) editions = remote
    }

    if (selected != null) {
        TremplinDetailScreen(selected!!) { selected = null }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(editions, key = { it.year }) { edition ->
            TremplinCard(edition, onClick = { selected = edition })
        }
    }
}

@Composable
fun TremplinCard(edition: TremplinEdition, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = TraverseYellow,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${edition.edition}${if (edition.edition == 1) "er" else "e"} Tremplin — ${edition.year}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (edition.date != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    edition.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (edition.upcoming) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Prochaine édition — lauréat annoncé après le tremplin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Vainqueur : ${edition.winners.joinToString(" / ")}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TraverseYellow
                )
                if (edition.participants.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Participants : ${edition.participants.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (edition.eventUrl != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Voir la fiche complète",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TremplinDetailScreen(edition: TremplinEdition, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${edition.edition}${if (edition.edition == 1) "er" else "e"} Tremplin — ${edition.year}"
                    )
                },
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
                .padding(20.dp)
        ) {
            if (edition.date != null) {
                Text(
                    edition.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
            }

            if (edition.upcoming) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("PROCHAINE ÉDITION", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Le ${edition.edition}e Tremplin de La Traverse se tiendra à La Traverse, Cléon. " +
                                "Les groupes sélectionnés auditionnent en public, en première partie d'un concert professionnel. " +
                                "Le vainqueur sera annoncé après le tremplin.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Text("VAINQUEUR", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TraverseYellow
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            edition.winners.joinToString(" / "),
                            style = MaterialTheme.typography.headlineMedium,
                            color = androidx.compose.ui.graphics.Color.Black
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("PARTICIPANTS", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (edition.participants.isEmpty()) {
                    Text(
                        "La liste des participants de cette édition n'a pas été publiée par la salle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    edition.participants.forEach { participant ->
                        val isWinner = edition.winners.any { w ->
                            w.lowercase().contains(participant.lowercase()) ||
                                participant.lowercase().contains(w.lowercase())
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isWinner) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(participant, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                if (isWinner) {
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        contentDescription = "Lauréat",
                                        tint = TraverseYellow
                                    )
                                }
                            }
                        }
                    }
                }

                if (edition.eventUrl != null) {
                    Spacer(Modifier.height(16.dp))
                    ActionRow(Icons.Filled.Web, "Page du lauréat sur latraverse.org") {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(edition.eventUrl))
                        )
                    }
                }
            }
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
