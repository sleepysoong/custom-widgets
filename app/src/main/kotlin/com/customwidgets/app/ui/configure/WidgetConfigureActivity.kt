package com.customwidgets.app.ui.configure

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.customwidgets.app.ui.create.CreateWidgetScreen
import com.customwidgets.app.ui.create.CreateWidgetViewModel
import com.customwidgets.app.ui.theme.CustomWidgetsTheme
import com.customwidgets.app.widget.WidgetManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WidgetConfigureActivity : ComponentActivity() {

    private val viewModel: CreateWidgetViewModel by viewModels()

    var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set result to CANCELED initially
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            CustomWidgetsTheme {
                CreateWidgetScreen(
                    viewModel = viewModel,
                    appWidgetId = appWidgetId,
                    onNavigateBack = { finish() },
                    onWidgetCreated = { savedWidgetId ->
                        CoroutineScope(Dispatchers.IO).launch {
                            WidgetManager.updateWidget(this@WidgetConfigureActivity, appWidgetId)
                        }
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}
