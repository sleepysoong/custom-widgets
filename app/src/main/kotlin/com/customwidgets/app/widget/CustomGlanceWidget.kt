package com.customwidgets.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.customwidgets.app.MainActivity
import com.customwidgets.app.data.local.WidgetDatabase
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.widget.renderer.DataBindingResolver
import com.customwidgets.app.widget.renderer.DslRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        val widgetDefinition = withContext(Dispatchers.IO) {
            try {
                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    WidgetDatabase::class.java,
                    WidgetDatabase.DATABASE_NAME
                ).build()
                val entity = db.widgetDao().getWidgetByAppWidgetId(appWidgetId)
                db.close()
                entity?.let { WidgetDefinition.fromJson(it.definitionJson) }
            } catch (_: Exception) {
                null
            }
        }

        provideContent {
            if (widgetDefinition != null) {
                DslRenderer.RenderWidget(
                    definition = widgetDefinition,
                    bindingResolver = DataBindingResolver()
                )
            } else {
                NotConfiguredPlaceholder()
            }
        }
    }
}

@Composable
private fun NotConfiguredPlaceholder() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF2C2C2C))
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = "Widget Not Configured",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Tap to setup widget",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFBB86FC)),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
