package com.gcap.client.ui.image

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gcap.client.ui.components.SafetySettingsPanel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageParameterPanel(
    uiState: ImageChatUiState,
    viewModel: ImageChatViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("图片模型配置 (Gemini 3.1 Flash Image)", style = MaterialTheme.typography.titleLarge)

            Text("图片配置 (imageConfig)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Aspect Ratio
            var aspectRatioExpanded by remember { mutableStateOf(false) }
            val aspectRatios = listOf(
                "auto" to "自动 (auto)",
                "1:1" to "1:1 (正方形)",
                "3:4" to "3:4 (竖向肖像)",
                "4:3" to "4:3 (横向风景)",
                "9:16" to "9:16 (竖屏海报)",
                "16:9" to "16:9 (横屏宽幅)"
            )
            ExposedDropdownMenuBox(
                expanded = aspectRatioExpanded,
                onExpandedChange = { aspectRatioExpanded = it }
            ) {
                OutlinedTextField(
                    value = aspectRatios.find { it.first == uiState.aspectRatio }?.second ?: "自动 (auto)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("宽高比例 (aspectRatio)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aspectRatioExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = aspectRatioExpanded,
                    onDismissRequest = { aspectRatioExpanded = false }
                ) {
                    aspectRatios.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.updateAspectRatio(value)
                                aspectRatioExpanded = false
                            }
                        )
                    }
                }
            }

            // Image Size
            Column {
                Text("分辨率 (imageSize)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                val sizes = listOf("1K", "2K")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    sizes.forEachIndexed { index, size ->
                        SegmentedButton(
                            selected = uiState.imageSize == size,
                            onClick = { viewModel.updateImageSize(size) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = sizes.size)
                        ) {
                            Text(size)
                        }
                    }
                }
            }

            // Output Mime Type
            Column {
                Text("图片格式 (imageOutputOptions.mimeType)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                val types = listOf("image/png" to "PNG", "image/jpeg" to "JPEG")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = uiState.outputMimeType == value,
                            onClick = { viewModel.updateOutputMimeType(value) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // Person Generation
            var personGenExpanded by remember { mutableStateOf(false) }
            val personGenOptions = listOf(
                "ALLOW_ALL" to "允许所有人像 (ALLOW_ALL)",
                "ALLOW_ADULT" to "仅允许成人肖像 (ALLOW_ADULT)",
                "DONT_ALLOW" to "禁止生成人像 (DONT_ALLOW)"
            )
            ExposedDropdownMenuBox(
                expanded = personGenExpanded,
                onExpandedChange = { personGenExpanded = it }
            ) {
                OutlinedTextField(
                    value = personGenOptions.find { it.first == uiState.personGeneration }?.second ?: "允许所有人像 (ALLOW_ALL)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("人物生成限制 (personGeneration)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = personGenExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = personGenExpanded,
                    onDismissRequest = { personGenExpanded = false }
                ) {
                    personGenOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.updatePersonGeneration(value)
                                personGenExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()
            Text("生成与采样参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Response Modalities (响应模态)
            Column {
                Text("响应模态 (responseModalities)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                val modalityPresets = listOf(
                    "图文混合" to listOf("TEXT", "IMAGE"),
                    "纯图片" to listOf("IMAGE"),
                    "纯文本" to listOf("TEXT")
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modalityPresets.forEachIndexed { index, (label, list) ->
                        SegmentedButton(
                            selected = uiState.responseModalities == list,
                            onClick = { viewModel.updateResponseModalities(list) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modalityPresets.size)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // Temperature
            Column {
                Text(
                    "采样温度 (Temperature): ${String.format(Locale.getDefault(), "%.2f", uiState.temperature)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.temperature,
                    onValueChange = { viewModel.updateTemperature(it) },
                    valueRange = 0f..2.0f
                )
            }

            // MaxOutputTokens
            Column {
                Text("最大输出 Tokens: ${uiState.maxOutputTokens}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.maxOutputTokens.toFloat(),
                    onValueChange = { viewModel.updateMaxOutputTokens(it.toInt()) },
                    valueRange = 1024f..32768f,
                    steps = 31
                )
            }

            // TopP
            Column {
                Text(
                    "Top P: ${String.format(Locale.getDefault(), "%.2f", uiState.topP)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.topP,
                    onValueChange = { viewModel.updateTopP(it) },
                    valueRange = 0f..1.0f
                )
            }

            // Thinking Level
            Column {
                Text("思考程度 (thinkingLevel)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                val levels = listOf("MINIMAL", "LOW", "MEDIUM", "HIGH")
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

            HorizontalDivider()
            SafetySettingsPanel(
                state = uiState.safetySettings,
                onStateChanged = { viewModel.updateSafetySettings(it) }
            )

            HorizontalDivider()
            Text("工具与联网", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Google 搜索检索", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.googleSearchEnabled,
                    onCheckedChange = { viewModel.updateGoogleSearchEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
