package com.articlevault.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.articlevault.data.AppStorage
import com.articlevault.data.ThemePreferences
import com.articlevault.ui.detail.ArticleDetailScreen
import com.articlevault.ui.folders.FolderScreen
import com.articlevault.ui.list.ArticleListScreen
import com.articlevault.ui.models.ModelSelectionScreen
import com.articlevault.ui.search.SearchScreen
import com.articlevault.ui.stats.StatsScreen
import com.articlevault.ui.storage.StoragePermissionScreen
import com.articlevault.ui.theme.ArticleVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var appStorage: AppStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(themePreferences.isDarkMode) }

            ArticleVaultTheme(darkTheme = isDarkMode) {
                var storageReady by remember { mutableStateOf(appStorage.isStoragePermissionGranted() && appStorage.hasExistingData()) }
                var permissionGranted by remember { mutableStateOf(appStorage.isStoragePermissionGranted()) }

                LaunchedEffect(permissionGranted) {
                    if (permissionGranted && !storageReady) {
                        appStorage.initializeStorage()
                        storageReady = true
                    }
                }

                if (!permissionGranted || !storageReady) {
                    StoragePermissionScreen(
                        storageRoot = appStorage.storageRoot.absolutePath,
                        hasExistingData = appStorage.hasExistingData(),
                        hasOldLocationData = appStorage.hasOldLocationData(),
                        onPermissionGranted = {
                            permissionGranted = true
                        },
                        onMigrateFromOldLocation = {
                            appStorage.migrateFromOldLocation()
                            permissionGranted = true
                        }
                    )
                } else {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "articles"
                    ) {
                        composable("articles") {
                            ArticleListScreen(
                                onArticleClick = { articleId ->
                                    navController.navigate("article/$articleId")
                                },
                                onNavigateToModels = { navController.navigate("models") },
                                onNavigateToFolders = { navController.navigate("folders") },
                                onNavigateToSearch = { navController.navigate("search") },
                                onNavigateToStats = { navController.navigate("stats") }
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
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("stats") {
                            StatsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("models") {
                            ModelSelectionScreen(
                                onBack = { navController.popBackStack() },
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = {
                                    themePreferences.toggleDarkMode()
                                    isDarkMode = themePreferences.isDarkMode
                                }
                            )
                        }
                        composable("folders") {
                            FolderScreen(
                                onBack = { navController.popBackStack() },
                                onFolderClick = {
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
