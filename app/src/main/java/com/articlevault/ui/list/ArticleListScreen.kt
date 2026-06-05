package com.articlevault.ui.list

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.articlevault.data.db.entity.Article
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ArticleListScreen(
    onArticleClick: (String) -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var articleToDelete by remember { mutableStateOf<Article?>(null) }
    val context = LocalContext.current

    if (showAddDialog) {
        AddLinkDialog(
            onDismiss = { showAddDialog = false },
            onSave = { url ->
                viewModel.saveArticle(url)
                Toast.makeText(context, "Saving: $url", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }

    articleToDelete?.let { article ->
        AlertDialog(
            onDismissRequest = { articleToDelete = null },
            title = { Text("Delete article?") },
            text = { Text("\"${article.title}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteArticle(article.id)
                    articleToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { articleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add link")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (articles.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(articles, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            onClick = { onArticleClick(article.id) },
                            onLongClick = { articleToDelete = article }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                text = "Your vault is empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Share a link from your browser or tap + to save an article for offline reading.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddLinkDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save article") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    isError = false
                },
                label = { Text("URL") },
                placeholder = { Text("https://example.com/article") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("Enter a valid URL starting with http:// or https://") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = url.trim()
                    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        onSave(trimmed)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = article.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (article.excerpt.isNotBlank()) {
                Text(
                    text = article.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTimestamp(article.savedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return when {
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> sdf.format(Date(millis))
    }
}
