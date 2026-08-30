package com.customwidgets.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.customwidgets.app.data.repository.WidgetRepository
import com.customwidgets.app.ui.create.CreateWidgetScreen
import com.customwidgets.app.ui.create.CreateWidgetViewModel
import com.customwidgets.app.ui.gallery.WidgetDetailScreen
import com.customwidgets.app.ui.gallery.WidgetGalleryScreen
import com.customwidgets.app.ui.gallery.WidgetGalleryViewModel
import com.customwidgets.app.ui.settings.ApiSettingsScreen
import com.customwidgets.app.ui.settings.ApiSettingsViewModel
import com.customwidgets.app.ui.theme.CustomWidgetsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

                NavHost(
                    navController = navController,
                    startDestination = "gallery"
                ) {
                    composable("gallery") {
                        val galleryViewModel: WidgetGalleryViewModel = hiltViewModel()
                        WidgetGalleryScreen(
                            viewModel = galleryViewModel,
                            onCreateWidgetClicked = { navController.navigate("create") },
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
