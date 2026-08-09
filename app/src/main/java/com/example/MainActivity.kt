package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CardDesignerScreen
import com.example.ui.screens.EditQuotesScreen
import com.example.ui.screens.FlowHomeScreen
import com.example.ui.screens.LibraryChronoScreen
import com.example.ui.screens.ProfileMetricsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.QuoteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: QuoteViewModel = viewModel()
            val currentTheme = viewModel.appThemeMode

            MyApplicationTheme(themeMode = currentTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("app_bottom_nav")
                        ) {
                            val items = listOf(
                                NavigationItem("flow", Icons.Default.Home, Icons.Outlined.Home, "Flow"),
                                NavigationItem("edit", Icons.Default.Edit, Icons.Outlined.Edit, "Edit"),
                                NavigationItem("designer", Icons.Default.Brush, Icons.Outlined.Brush, "Designer"),
                                NavigationItem("library", Icons.Default.LibraryBooks, Icons.Outlined.LibraryBooks, "Library"),
                                NavigationItem("profile", Icons.Default.Person, Icons.Outlined.Person, "Me")
                            )

                            items.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("nav_btn_${item.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "flow",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("flow") {
                            FlowHomeScreen(viewModel = viewModel)
                        }
                        composable("edit") {
                            EditQuotesScreen(viewModel = viewModel)
                        }
                        composable("designer") {
                            CardDesignerScreen(viewModel = viewModel)
                        }
                        composable("library") {
                            LibraryChronoScreen(viewModel = viewModel)
                        }
                        composable("profile") {
                            ProfileMetricsScreen(
                                viewModel = viewModel,
                                themeMode = currentTheme,
                                onThemeChange = { viewModel.updateThemeMode(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
