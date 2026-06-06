package com.articlevault.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.articlevault.ui.detail.ArticleDetailScreen
import com.articlevault.ui.folders.FolderScreen
import com.articlevault.ui.list.ArticleListScreen
import com.articlevault.ui.models.ModelSelectionScreen
import com.articlevault.ui.search.SearchScreen
import com.articlevault.ui.stats.StatsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArticleVaultTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isDetailScreen = currentRoute?.startsWith("article/") == true
                val isModelsScreen = currentRoute == "models"
                val isFoldersScreen = currentRoute == "folders"

                Scaffold(
                    bottomBar = {
                        if (!isDetailScreen && !isModelsScreen && !isFoldersScreen) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                    label = { Text("Articles") },
                                    selected = currentRoute == "articles",
                                    onClick = {
                                        navController.navigate("articles") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    label = { Text("Search") },
                                    selected = currentRoute == "search",
                                    onClick = {
                                        navController.navigate("search") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                                    label = { Text("Stats") },
                                    selected = currentRoute == "stats",
                                    onClick = {
                                        navController.navigate("stats") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text("Settings") },
                                    selected = currentRoute == "models",
                                    onClick = {
                                        navController.navigate("models") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "articles",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("articles") {
                            ArticleListScreen(
                                onArticleClick = { articleId ->
                                    navController.navigate("article/$articleId")
                                },
                                onNavigateToModels = { navController.navigate("models") },
                                onNavigateToFolders = { navController.navigate("folders") }
                            )
                        }
                        composable("article/{articleId}") { backStackEntry ->
                            val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
                            ArticleDetailScreen(
                                articleId = articleId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                onArticleClick = { articleId ->
                                    navController.navigate("article/$articleId")
                                }
                            )
                        }
                        composable("stats") {
                            StatsScreen()
                        }
                        composable("models") {
                            ModelSelectionScreen(onBack = { navController.popBackStack() })
                        }
                        composable("folders") {
                            FolderScreen(
                                onBack = { navController.popBackStack() },
                                onFolderClick = { folderId ->
                                    navController.navigate("articles") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
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

@Composable
private fun ArticleVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}
