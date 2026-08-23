package com.gcap.client.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gcap.client.data.model.ModelRequestFormat
import com.gcap.client.ui.components.SafetySettingsPanel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterPanel(
    uiState: ChatUiState,
    viewModel: ChatViewModel,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("模型参数配置", style = MaterialTheme.typography.titleLarge)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = uiState.selectedModel.providerName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "当前选中模型: ${uiState.selectedModel.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 系统指令 (System Instruction)
            if (uiState.selectedModel.supportsSystemInstruction) {
                OutlinedTextField(
                    value = uiState.systemInstruction,
                    onValueChange = { viewModel.updateSystemInstruction(it) },
                    label = { Text("系统指令 (System Instruction)") },
                    placeholder = { Text("例如：You are a helpful coding assistant...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            HorizontalDivider()
            Text("生成与采样参数", style = MaterialTheme.typography.titleMedium)

            // Max Output Tokens
            Column {
                Text("最大输出 Tokens: ${uiState.maxOutputTokens}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.maxOutputTokens.toFloat(),
                    onValueChange = { viewModel.updateMaxOutputTokens(it.toInt()) },
                    valueRange = 1024f..65535f,
                    steps = 63
                )
            }

            // Temperature (if supported)
            if (uiState.selectedModel.supportsTemperature) {
                Column {
                    Text(
                        "采样温度 (Temperature): ${String.format(Locale.getDefault(), "%.2f", uiState.temperature)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.temperature,
                        onValueChange = { viewModel.updateTemperature(it) },
                        valueRange = 0.0f..2.0f,
                        steps = 39
                    )
                }
            }

            // Top P (if supported)
            if (uiState.selectedModel.supportsTopP) {
                Column {
                    Text(
                        "Top P: ${String.format(Locale.getDefault(), "%.2f", uiState.topP)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.topP,
                        onValueChange = { viewModel.updateTopP(it) },
                        valueRange = 0.0f..1.0f,
                        steps = 19
                    )
                }
            }

            // Thinking Level (for Google models with Thinking)
            if (uiState.selectedModel.requestFormat == ModelRequestFormat.FORMAT_ONE || uiState.selectedModel.requestFormat == ModelRequestFormat.FORMAT_TWO) {
                Column {
                    Text("思考级别 (Thinking Level)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    val levels = listOf("NONE", "MINIMAL", "MEDIUM", "HIGH")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        levels.forEachIndexed { index, level ->
                            SegmentedButton(
                                selected = uiState.thinkingLevel == level,
                                onClick = { viewModel.updateThinkingLevel(level) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = levels.size)
                            ) {
                                Text(level)
                            }
                        }
                    }
                }
            }

            // Safety Settings & Google Tools (Google Vertex AI specific)
            if (!uiState.selectedModel.isCustom) {
                HorizontalDivider()
                SafetySettingsPanel(
                    state = uiState.safetySettings,
                    onStateChanged = { viewModel.updateSafetySettings(it) }
                )

                HorizontalDivider()
                Text("内置工具与检索", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Google 搜索检索", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = uiState.googleSearchEnabled,
                        onCheckedChange = { viewModel.updateGoogleSearch(it) }
                    )
                }

                if (uiState.selectedModel.supportsGoogleMaps) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Google 地图工具", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = uiState.googleMapsEnabled,
                            onCheckedChange = { viewModel.updateGoogleMaps(it) }
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.toolConfigLanguageCode,
                    onValueChange = { viewModel.updateToolConfigLanguage(it) },
                    label = { Text("检索语言代码 (toolConfig.languageCode)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
