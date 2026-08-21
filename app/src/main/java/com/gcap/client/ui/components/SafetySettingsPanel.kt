package com.gcap.client.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SafetySettingsState(
    val hateSpeech: String = "OFF",
    val dangerousContent: String = "OFF",
    val sexuallyExplicit: String = "OFF",
    val harassment: String = "OFF"
)

val SafetyThresholds = listOf(
    "OFF" to "关闭 (OFF)",
    "BLOCK_LOW_AND_ABOVE" to "低及以上阻止",
    "BLOCK_MEDIUM_AND_ABOVE" to "中及以上阻止",
    "BLOCK_HIGH_AND_ABOVE" to "高及以上阻止",
    "BLOCK_NONE" to "不阻止 (BLOCK_NONE)"
)

@Composable
fun SafetySettingsPanel(
    state: SafetySettingsState,
    onStateChanged: (SafetySettingsState) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("安全拦截设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            SafetyThresholdDropdown(
                label = "仇恨言论 (HATE_SPEECH)",
                selectedThreshold = state.hateSpeech,
                onThresholdSelected = { onStateChanged(state.copy(hateSpeech = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SafetyThresholdDropdown(
                label = "危险内容 (DANGEROUS_CONTENT)",
                selectedThreshold = state.dangerousContent,
                onThresholdSelected = { onStateChanged(state.copy(dangerousContent = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SafetyThresholdDropdown(
                label = "露骨色情 (SEXUALLY_EXPLICIT)",
                selectedThreshold = state.sexuallyExplicit,
                onThresholdSelected = { onStateChanged(state.copy(sexuallyExplicit = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SafetyThresholdDropdown(
                label = "骚扰内容 (HARASSMENT)",
                selectedThreshold = state.harassment,
                onThresholdSelected = { onStateChanged(state.copy(harassment = it)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyThresholdDropdown(
    label: String,
    selectedThreshold: String,
    onThresholdSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = SafetyThresholds.find { it.first == selectedThreshold }?.second ?: selectedThreshold

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SafetyThresholds.forEach { (threshold, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onThresholdSelected(threshold)
                        expanded = false
                    }
                )
            }
        }
    }
}
