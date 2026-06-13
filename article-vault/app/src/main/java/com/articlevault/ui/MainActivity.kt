package com.articlevault.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.articlevault.data.ThemePreferences
import com.articlevault.ui.detail.ArticleDetailScreen
import com.articlevault.ui.folders.FolderScreen
import com.articlevault.ui.list.ArticleListScreen
import com.articlevault.ui.models.ModelSelectionScreen
import com.articlevault.ui.search.SearchScreen
import com.articlevault.ui.stats.StatsScreen
import com.articlevault.ui.theme.ArticleVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArticleVaultTheme(darkTheme = themePreferences.isDarkMode) {
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
                            }
                        )
                    }
                    composable("stats") {
                        StatsScreen(onBack = { navController.popBackStack() })
                    }
                    composable("models") {
                        ModelSelectionScreen(
                            onBack = { navController.popBackStack() },
                            onToggleDarkMode = {
                                themePreferences.toggleDarkMode()
                                this@MainActivity.recreate()
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
