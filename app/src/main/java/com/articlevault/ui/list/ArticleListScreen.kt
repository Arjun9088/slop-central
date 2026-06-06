package com.articlevault.ui.list

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.articlevault.data.db.entity.Article
import com.articlevault.ml.DomainClassifier
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (String) -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToFolders: () -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val duplicateWarning by viewModel.duplicateWarning.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var articleToDelete by remember { mutableStateOf<Article?>(null) }
    var contextMenuArticle by remember { mutableStateOf<Article?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var summaryArticle by remember { mutableStateOf<Article?>(null) }
    var summaryText by remember { mutableStateOf<String?>(null) }
    var isSummarizing by remember { mutableStateOf(false) }
    var summaryError by remember { mutableStateOf<String?>(null) }
    var showMoveToFolder by remember { mutableStateOf(false) }
    var batchDeleteConfirm by remember { mutableStateOf(false) }

    // Duplicate warning dialog
    duplicateWarning?.let { title ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateWarning() },
            title = { Text("Already saved") },
            text = { Text("\"$title\" is already in your vault. Save anyway?") },
            confirmButton = {
                TextButton(onClick = { viewModel.saveArticleForce("") }) { Text("Save again") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicateWarning() }) { Text("Cancel") }
            }
        )
    }

    if (showAddDialog) {
        AddLinkDialog(
            onDismiss = { showAddDialog = false },
            onSave = { url ->
                viewModel.saveArticle(url)
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
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { articleToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Batch delete confirmation
    if (batchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { batchDeleteConfirm = false },
            title = { Text("Delete ${selectedIds.size} articles?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.batchDelete()
                    batchDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { batchDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Move to folder dialog
    if (showMoveToFolder) {
        AlertDialog(
            onDismissRequest = { showMoveToFolder = false },
            title = { Text("Move to folder") },
            text = {
                Column {
                    folders.forEach { folder ->
                        TextButton(
                            onClick = {
                                viewModel.batchMoveToFolder(folder.id)
                                showMoveToFolder = false
                                Toast.makeText(context, "Moved ${selectedIds.size} articles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(folder.name)
                        }
                    }
                    if (folders.isEmpty()) {
                        Text("No folders yet. Create one in Folders.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveToFolder = false }) { Text("Cancel") }
            }
        )
    }

    // Long-press context menu
    if (showContextMenu && contextMenuArticle != null) {
        val article = contextMenuArticle!!
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = {
                Text(article.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium)
            },
            text = { Text("What would you like to do?") },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = {
                            showContextMenu = false
                            summaryArticle = article
                            summaryText = article.summary.ifBlank { null }
                            summaryError = null
                            isSummarizing = article.summary.isBlank()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (article.summary.isBlank()) "Summarize" else "View summary")
                    }
                    TextButton(
                        onClick = {
                            showContextMenu = false
                            viewModel.toggleRead(article.id, article.read)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (article.read) Icons.Default.CheckCircle else Icons.Default.Done,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (article.read) "Mark as unread" else "Mark as read")
                    }
                    TextButton(
                        onClick = {
                            showContextMenu = false
                            viewModel.enterMultiSelect(article.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select for batch action")
                    }
                    TextButton(
                        onClick = {
                            showContextMenu = false
                            articleToDelete = article
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showContextMenu = false }) { Text("Cancel") }
            }
        )
    }

    // Summary dialog
    if (summaryArticle != null) {
        val article = summaryArticle!!
        AlertDialog(
            onDismissRequest = {
                summaryArticle = null; summaryText = null; summaryError = null; isSummarizing = false
            },
            title = {
                Column {
                    Text("Summary", style = MaterialTheme.typography.titleMedium)
                    if (viewModel.getSelectedModelName() != null) {
                        Text("Model: ${viewModel.getSelectedModelName()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                when {
                    isSummarizing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Generating summary...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    summaryError != null -> {
                        Column {
                            Text(summaryError!!, color = MaterialTheme.colorScheme.error)
                            if (summaryError!!.contains("No AI model")) {
                                TextButton(onClick = { summaryArticle = null; onNavigateToModels() }) {
                                    Text("Go to Models")
                                }
                            }
                        }
                    }
                    summaryText != null -> {
                        Text(summaryText!!, modifier = Modifier.verticalScroll(rememberScrollState()))
                    }
                }
            },
            confirmButton = {
                Row {
                    if (summaryText != null && !isSummarizing) {
                        TextButton(onClick = {
                            summaryText = null; summaryError = null; isSummarizing = true
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-generate")
                        }
                    }
                    TextButton(onClick = {
                        summaryArticle = null; summaryText = null; summaryError = null; isSummarizing = false
                    }) { Text("Close") }
                }
            }
        )
        LaunchedEffect(isSummarizing) {
            if (isSummarizing && summaryArticle != null) {
                val result = viewModel.summarize(summaryArticle!!.id)
                result.fold(
                    onSuccess = { summaryText = it; summaryError = null },
                    onFailure = { summaryError = it.message; summaryText = null }
                )
                isSummarizing = false
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isMultiSelectMode,
                enter = slideInVertically(),
                exit = slideOutVertically()
            ) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitMultiSelect() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Select all")
                        }
                        IconButton(onClick = { batchDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                        IconButton(onClick = {
                            viewModel.batchMarkRead()
                            Toast.makeText(context, "Marked as read", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Done, contentDescription = "Mark read")
                        }
                        IconButton(onClick = { showMoveToFolder = true }) {
                            Icon(Icons.Default.Star, contentDescription = "Move to folder")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add link")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == ListFilter.ALL && selectedFolderId == null && selectedTag == null,
                    onClick = { viewModel.setFilter(ListFilter.ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter == ListFilter.UNREAD,
                    onClick = { viewModel.setFilter(ListFilter.UNREAD) },
                    label = { Text("Unread") }
                )
                FilterChip(
                    selected = filter == ListFilter.READ,
                    onClick = { viewModel.setFilter(ListFilter.READ) },
                    label = { Text("Read") }
                )
                // Tag filters
                tags.take(6).forEach { tag ->
                    FilterChip(
                        selected = selectedTag == tag.name,
                        onClick = { viewModel.selectTag(if (selectedTag == tag.name) null else tag.name) },
                        label = { Text(tag.name) }
                    )
                }
                // Folder filters
                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { viewModel.selectFolder(folder.id) },
                        label = { Text(folder.name) }
                    )
                }
                AssistChip(
                    onClick = onNavigateToFolders,
                    label = { Text("Folders") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Article list
            if (articles.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(articles, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            isSelected = selectedIds.contains(article.id),
                            isMultiSelectMode = isMultiSelectMode,
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSelection(article.id)
                                } else {
                                    onArticleClick(article.id)
                                }
                            },
                            onLongClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSelection(article.id)
                                } else {
                                    contextMenuArticle = article
                                    showContextMenu = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            Text("Your vault is empty", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold)
            Text("Share a link from your browser or tap + to save an article.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AddLinkDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save article") },
        text = {
            OutlinedTextField(
                value = url, onValueChange = { url = it; isError = false },
                label = { Text("URL") },
                placeholder = { Text("https://example.com/article") },
                singleLine = true, isError = isError,
                supportingText = if (isError) {{ Text("Enter a valid URL") }} else null,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = url.trim()
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) onSave(trimmed)
                else isError = true
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArticleCard(
    article: Article,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val readingTime = DomainClassifier.extractReadingTimeMinutes(article.wordCount)
    val siteType = if (article.domain.isNotBlank()) DomainClassifier.classify(article.url).label else null

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (isSelected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (article.read) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!article.read) {
                    Icon(Icons.Default.Favorite, contentDescription = "Unread",
                        modifier = Modifier.size(16.dp).padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                    }
                )
                if (siteType != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = siteType,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (article.summary.isNotBlank()) {
                Text(text = article.summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else if (article.excerpt.isNotBlank()) {
                Text(text = article.excerpt, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = formatTimestamp(article.savedAt), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (article.wordCount > 0) {
                        Text(
                            text = "$readingTime min read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (article.readingProgress > 0.01f) {
                        Text(
                            text = "${(article.readingProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            // Reading progress bar
            if (article.readingProgress > 0.01f) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { article.readingProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
    }
}
