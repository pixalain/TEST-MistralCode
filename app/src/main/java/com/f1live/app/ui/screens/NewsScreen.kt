package com.f1live.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f1live.app.R
import com.f1live.app.data.model.Race
import com.f1live.app.data.repository.NewsItem
import com.f1live.app.ui.NewsUi
import com.f1live.app.ui.UiState

@Composable
fun NewsScreen(
    state: UiState<NewsUi>,
    onRefresh: () -> Unit
) {
    when (state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(state.message, onRefresh)
        is UiState.Success -> NewsContent(state.data)
    }
}

@Composable
private fun NewsContent(data: NewsUi) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SectionHeader(stringResource(R.string.news_last_gp, data.lastRace?.raceName ?: "—")) }
        if (data.lastNews.isEmpty()) {
            item { EmptyNote(stringResource(R.string.news_empty)) }
        } else {
            items(data.lastNews, key = { "last-${it.title}" }) { NewsCard(it) }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item { SectionHeader(stringResource(R.string.news_next_gp, data.nextRace?.raceName ?: "—")) }
        if (data.nextNews.isEmpty()) {
            item { EmptyNote(stringResource(R.string.news_empty)) }
        } else {
            items(data.nextNews, key = { "next-${it.title}" }) { NewsCard(it) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun NewsCard(item: NewsItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (item.link.isNotBlank()) {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(item.source)
                        item.date?.let { append(" · ").append(it.take(16)) }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
