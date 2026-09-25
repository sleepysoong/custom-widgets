package com.customwidgets.app.ui.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.customwidgets.app.R

@Composable
fun GlassAppearanceSettings(modifier: Modifier = Modifier) {
    val settings = LocalGlassSettings.current ?: return
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.glass_appearance_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.glass_appearance_description), style = MaterialTheme.typography.bodyMedium)
            GlassPreference.entries.forEach { option ->
                val label = when (option) {
                    GlassPreference.Auto -> R.string.glass_automatic
                    GlassPreference.Reduced -> R.string.glass_reduced
                    GlassPreference.Off -> R.string.glass_off
                }
                GlassFilterChip(
                    selected = settings.preference == option,
                    onClick = { settings.select(option) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
