package com.customwidgets.app.qa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.customwidgets.app.ui.glass.*
import com.customwidgets.app.ui.theme.CustomWidgetsTheme

/** Debug-only entry point: no network, generated widgets or AI key required. */
class GlassCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dark by rememberSaveable { mutableStateOf(false) }
            CustomWidgetsTheme(darkTheme = dark, dynamicColor = false) {
                Column(
                    Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Liquid Glass catalog", style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassFilterChip(dark, { dark = !dark }, { Text("Dark theme") })
                        GlassSecondaryButton({ recreate() }) { Text("Recreate") }
                    }
                    CatalogControls()
                }
            }
        }
    }
}

@Composable
private fun CatalogControls() {
    var clicks by rememberSaveable { mutableIntStateOf(0) }
    var checked by rememberSaveable { mutableStateOf(false) }
    var value by rememberSaveable { mutableFloatStateOf(.5f) }
    var text by rememberSaveable { mutableStateOf("") }
    var dialog by rememberSaveable { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth().testTag("glass-card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Clicks: $clicks", Modifier.testTag("click-count"))
            GlassButton({ clicks++ }, Modifier.testTag("glass-button")) { Text("Primary") }
            GlassSecondaryButton({}, enabled = false, modifier = Modifier.testTag("disabled-button")) { Text("Disabled") }
            GlassTextField(text, { text = it }, label = { Text("Input") }, modifier = Modifier.testTag("glass-input"))
            GlassSwitch(checked, { checked = it }, Modifier.testTag("glass-switch"))
            GlassSlider(value, { value = it }, Modifier.testTag("glass-slider"))
            GlassButton({ dialog = true }, Modifier.testTag("open-dialog")) { Text("Open dialog") }
        }
    }
    if (dialog) {
        GlassDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Glass dialog") },
            text = { GlassTextField(text, { text = it }, label = { Text("Dialog input") }) },
            confirmButton = { GlassButton({ dialog = false }, Modifier.testTag("close-dialog")) { Text("Done") } }
        )
    }
}
