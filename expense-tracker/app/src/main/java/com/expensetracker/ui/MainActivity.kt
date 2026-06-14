package com.expensetracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expensetracker.data.sync.SyncPreferences
import com.expensetracker.data.sync.ThemeMode
import com.expensetracker.ui.dashboard.DashboardScreen
import com.expensetracker.ui.entry.EntryScreen
import com.expensetracker.ui.list.ExpenseListScreen
import com.expensetracker.ui.receipt.ReceiptScreen
import com.expensetracker.ui.settings.SettingsScreen
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var syncPreferences: SyncPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(syncPreferences.getThemeMode()) }

            ExpenseTrackerTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "expenses"
                ) {
                    composable("expenses") {
                        ExpenseListScreen(
                            onAddExpense = { navController.navigate("entry") },
                            onEditExpense = { id -> navController.navigate("entry/$id") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToDashboard = { navController.navigate("dashboard") },
                            onNavigateToReceipt = { navController.navigate("receipt") }
                        )
                    }
                    composable("entry") {
                        EntryScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        "entry/{expenseId}",
                        arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
                        EntryScreen(
                            expenseId = expenseId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onThemeChanged = { mode ->
                                syncPreferences.setThemeMode(mode)
                                themeMode = mode
                            }
                        )
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("receipt") {
                        ReceiptScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

}
