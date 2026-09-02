package com.customwidgets.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.customwidgets.app.data.repository.WidgetRepository
import com.customwidgets.app.ui.create.CreateWidgetScreen
import com.customwidgets.app.ui.create.CreateWidgetViewModel
import com.customwidgets.app.ui.gallery.WidgetDetailScreen
import com.customwidgets.app.ui.gallery.WidgetGalleryScreen
import com.customwidgets.app.ui.gallery.WidgetGalleryViewModel
import com.customwidgets.app.ui.mcp.McpServerScreen
import com.customwidgets.app.ui.mcp.McpServerViewModel
import com.customwidgets.app.ui.settings.ApiSettingsScreen
import com.customwidgets.app.ui.settings.ApiSettingsViewModel
import com.customwidgets.app.ui.theme.CustomWidgetsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Gallery : BottomNavItem("gallery", "내 위젯", Icons.Filled.Home, Icons.Outlined.Home)
    data object Create : BottomNavItem("create", "만들기", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline)
    data object Mcp : BottomNavItem("mcp", "MCP 도구", Icons.Filled.Build, Icons.Outlined.Build)
    data object Settings : BottomNavItem("settings", "설정", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val NAV_ITEMS = listOf(
    BottomNavItem.Gallery,
    BottomNavItem.Create,
    BottomNavItem.Mcp,
    BottomNavItem.Settings
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var widgetRepository: WidgetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomWidgetsTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        val isBottomBarVisible = currentRoute in listOf("gallery", "create", "mcp", "settings")
                        if (isBottomBarVisible) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                NAV_ITEMS.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "gallery",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("gallery") {
                            val galleryViewModel: WidgetGalleryViewModel = hiltViewModel()
                            WidgetGalleryScreen(
                                viewModel = galleryViewModel,
                                onCreateWidgetClicked = { navController.navigate("create") },
                                onMcpClicked = { navController.navigate("mcp") },
                                onSettingsClicked = { navController.navigate("settings") },
                                onWidgetClicked = { widgetId -> navController.navigate("detail/$widgetId") }
                            )
                        }

                        composable("create") {
                            val createViewModel: CreateWidgetViewModel = hiltViewModel()
                            CreateWidgetScreen(
                                viewModel = createViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onWidgetCreated = {
                                    navController.popBackStack("gallery", inclusive = false)
                                }
                            )
                        }

                        composable("mcp") {
                            val mcpViewModel: McpServerViewModel = hiltViewModel()
                            McpServerScreen(
                                viewModel = mcpViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "detail/{widgetId}",
                            arguments = listOf(navArgument("widgetId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val widgetId = backStackEntry.arguments?.getLong("widgetId") ?: 0L
                            WidgetDetailScreen(
                                widgetId = widgetId,
                                repository = widgetRepository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            val settingsViewModel: ApiSettingsViewModel = hiltViewModel()
                            ApiSettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
