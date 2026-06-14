package com.articlevault.ui.list

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (String) -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStats: () -> Unit,
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                            Spacer(modifier = Modifier.width(12.dp))
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
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
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
                        Spacer(modifier = Modifier.width(12.dp))
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
                        Spacer(modifier = Modifier.width(12.dp))
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showContextMenu = false }) { Text("Cancel") }
            }
        )
    }

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
                                    Text("Go to Settings")
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
                            Spacer(modifier = Modifier.width(6.dp))
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                onArticles = { scope.launch { drawerState.close() } },
                onSearch = { scope.launch { drawerState.close() }; onNavigateToSearch() },
                onFolders = { scope.launch { drawerState.close() }; onNavigateToFolders() },
                onStats = { scope.launch { drawerState.close() }; onNavigateToStats() },
                onSettings = { scope.launch { drawerState.close() }; onNavigateToModels() }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (isMultiSelectMode) {
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
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                "Articles",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToSearch) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            floatingActionButton = {
                if (!isMultiSelectMode) {
                                    ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    text = { Text("Add") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                FilterChipsRow(
                    filter = filter,
                    selectedFolderId = selectedFolderId,
                    selectedTag = selectedTag,
                    folders = folders,
                    tags = tags,
                    onFilterChange = { viewModel.setFilter(it) },
                    onTagSelect = { tag -> viewModel.selectTag(if (selectedTag == tag) null else tag) },
                    onFolderSelect = { viewModel.selectFolder(it) }
                )

                if (articles.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp)
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
}

// ──────────────────────────────────────────────
// Drawer
// ──────────────────────────────────────────────
@Composable
private fun AppDrawer(
    onArticles: () -> Unit,
    onSearch: () -> Unit,
    onFolders: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "ArticleVault",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        NavigationDrawerItem(
            label = { Text("Articles") },
            selected = true,
            onClick = onArticles,
            icon = { Icon(Icons.Default.Star, contentDescription = null) },
            colors = drawerItemColors()
        )
        NavigationDrawerItem(
            label = { Text("Search") },
            selected = false,
            onClick = onSearch,
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = drawerItemColors()
        )
        NavigationDrawerItem(
            label = { Text("Folders") },
            selected = false,
            onClick = onFolders,
            icon = { Icon(Icons.Default.Star, contentDescription = null) },
            colors = drawerItemColors()
        )
        NavigationDrawerItem(
            label = { Text("Stats") },
            selected = false,
            onClick = onStats,
            icon = { Icon(Icons.Default.Star, contentDescription = null) },
            colors = drawerItemColors()
        )
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = false,
            onClick = onSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            colors = drawerItemColors()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun drawerItemColors() = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    unselectedContainerColor = Color.Transparent,
    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedTextColor = MaterialTheme.colorScheme.onSurface
)

// ──────────────────────────────────────────────
// Filter chips
// ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    filter: ListFilter,
    selectedFolderId: Long?,
    selectedTag: String?,
    folders: List<com.articlevault.data.db.entity.Folder>,
    tags: List<com.articlevault.data.db.entity.Tag>,
    onFilterChange: (ListFilter) -> Unit,
    onTagSelect: (String) -> Unit,
    onFolderSelect: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filter == ListFilter.ALL && selectedFolderId == null && selectedTag == null,
            onClick = { onFilterChange(ListFilter.ALL); onFolderSelect(null) },
            label = { Text("All") }
        )
        FilterChip(
            selected = filter == ListFilter.UNREAD,
            onClick = { onFilterChange(ListFilter.UNREAD) },
            label = { Text("Unread") }
        )
        FilterChip(
            selected = filter == ListFilter.READ,
            onClick = { onFilterChange(ListFilter.READ) },
            label = { Text("Read") }
        )
        if (folders.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        folders.forEach { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onFolderSelect(if (selectedFolderId == folder.id) null else folder.id) },
                label = { Text(folder.name) }
            )
        }
        if (tags.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        tags.take(6).forEach { tag ->
            FilterChip(
                selected = selectedTag == tag.name,
                onClick = { onTagSelect(tag.name) },
                label = { Text(tag.name) }
            )
        }
    }
}

// ──────────────────────────────────────────────
// Empty state
// ──────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Your vault is empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Share a link from your browser or tap + to save an article.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ──────────────────────────────────────────────
// Add link dialog
// ──────────────────────────────────────────────
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

// ──────────────────────────────────────────────
// Article card
// ──────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArticleCard(
    article: Article,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val readingTime = remember(article.wordCount) { DomainClassifier.extractReadingTimeMinutes(article.wordCount) }
    val timestamp = remember(article.savedAt) { formatTimestamp(article.savedAt) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null,
        tonalElevation = if (isSelected) 0.dp else 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(20.dp)
                )
            } else if (!article.read) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp, top = 4.dp)
                        .width(3.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else {
                Spacer(modifier = Modifier.width(3.dp).padding(end = 12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (article.read) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (article.read)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (article.domain.isNotBlank()) {
                        Text(
                            text = article.domain,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (article.wordCount > 0) {
                        if (article.domain.isNotBlank()) {
                            Text(
                                text = " · ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = "$readingTime min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (article.excerpt.isNotBlank() || article.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (article.summary.isNotBlank()) article.summary else article.excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (article.readingProgress > 0.01f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { article.readingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.DAYS.toDays(diff)}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}
