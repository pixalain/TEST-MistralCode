package com.f1live.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f1live.app.R
import com.f1live.app.data.repository.Regulations
import com.f1live.app.ui.UiState

@Composable
fun RegulationsScreen(
    state: UiState<Regulations>,
    onRefresh: () -> Unit,
    lastUpdated: Long? = null
) {
    when (state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(state.message, onRefresh)
        is UiState.Success -> {
            if (state.data.categories.isEmpty()) {
                ErrorBox(stringResource(R.string.no_regulations), onRefresh)
                return
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.regulations_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    FreshnessBadge(state.fromCache, lastUpdated)
                }
                items(state.data.categories, key = { it.title }) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            cat.sections.forEach { sec ->
                                Text(
                                    text = sec.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                sec.points.forEach { p ->
                                    Text(
                                        text = "• $p",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 3.dp, start = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
