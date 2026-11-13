package com.example.infoconcert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infoconcert.data.Concert
import com.example.infoconcert.data.Department
import com.example.infoconcert.ui.ConcertsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ConcertsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(viewModel: ConcertsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val department = state.department

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (department == null) "Concerts par département" else "Concerts ${department.label}")
                },
                navigationIcon = {
                    if (department != null) {
                        IconButton(onClick = viewModel::backToDepartments) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (department == null) {
            DepartmentPickerScreen(viewModel, Modifier.padding(padding))
        } else {
            ConcertListScreen(state, Modifier.padding(padding))
        }
    }
}

@Composable
fun DepartmentPickerScreen(viewModel: ConcertsViewModel, modifier: Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val departments = viewModel.filteredDepartments(query)

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Rechercher un département…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(departments, key = { it.code }) { department ->
                DepartmentRow(department, onClick = { viewModel.selectDepartment(department) })
            }
        }
    }
}

@Composable
fun DepartmentRow(department: Department, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = department.code,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Text(department.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ConcertListScreen(state: com.example.infoconcert.ui.ConcertsUiState, modifier: Modifier) {
    when {
        state.isLoading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Chargement des concerts…")
            }
        }
        state.error != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            }
        }
        state.concerts.isEmpty() -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aucun concert trouvé pour ce département.")
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.concerts, key = { it.infoUrl }) { concert ->
                    ConcertCard(concert)
                }
            }
        }
    }
}

@Composable
fun ConcertCard(concert: Concert) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(concert.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (concert.venue.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Lieu : ${concert.venue}", style = MaterialTheme.typography.bodyMedium)
            }
            if (concert.city.isNotBlank()) {
                Text("Ville : ${concert.city}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Date : ${concert.date}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Heure : ${concert.time ?: "non communiquée"}",
                style = MaterialTheme.typography.bodyMedium
            )
            concert.price?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { openUrl(context, concert.bookingUrl ?: concert.infoUrl) },
                    enabled = concert.bookingUrl != null
                ) {
                    Text(if (concert.bookingUrl != null) "Réserver" else "Non réservable")
                }
                Button(onClick = { openUrl(context, concert.infoUrl) }) {
                    Text("+ d'infos")
                }
            }
        }
    }
}

fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
