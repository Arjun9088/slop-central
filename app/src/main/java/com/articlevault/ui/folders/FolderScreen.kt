package com.articlevault.ui.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.articlevault.data.db.entity.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    onBack: () -> Unit,
    onFolderClick: (Long) -> Unit,
    viewModel: FolderViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<Folder?>(null) }
    var deletingFolder by remember { mutableStateOf<Folder?>(null) }

    // Create dialog
    if (showCreateDialog) {
        FolderNameDialog(
            title = "New folder",
            initialName = "",
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Rename dialog
    editingFolder?.let { folder ->
        FolderNameDialog(
            title = "Rename folder",
            initialName = folder.name,
            onConfirm = { name ->
                viewModel.renameFolder(folder.id, name)
                editingFolder = null
            },
            onDismiss = { editingFolder = null }
        )
    }

    // Delete dialog
    deletingFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("Delete folder?") },
            text = { Text("\"${folder.name}\" will be removed. Articles inside won't be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    deletingFolder = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingFolder = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New folder")
            }
        }
    ) { padding ->
        if (folders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                    Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No folders yet", style = MaterialTheme.typography.bodyLarge)
                    Text("Tap + to create one", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders, key = { it.id }) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { onFolderClick(folder.id) },
                        onRename = { editingFolder = folder },
                        onDelete = { deletingFolder = folder }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: Folder,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    isError = false
                },
                label = { Text("Folder name") },
                singleLine = true,
                isError = isError,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    isError = true
                } else {
                    onConfirm(name)
                }
            }) {
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
