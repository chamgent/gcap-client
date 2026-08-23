package com.gcap.client.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gcap.client.data.local.ConversationDao
import com.gcap.client.data.local.SettingsDataStore
import com.gcap.client.data.model.ModelRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val conversationDao: ConversationDao,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    val apiKey = settingsDataStore.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val themeMode = settingsDataStore.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val dynamicColor = settingsDataStore.dynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultChatModel = settingsDataStore.defaultChatModelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-3.7-flash")

    val defaultImageModel = settingsDataStore.defaultImageModelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-3.1-flash-image")

    private val _isValidating = MutableStateFlow(false)
    val isValidating = _isValidating.asStateFlow()

    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult = _validationResult.asStateFlow()

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.setApiKey(key)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDynamicColor(enabled)
        }
    }

    fun updateDefaultChatModel(modelId: String) {
        viewModelScope.launch {
            settingsDataStore.setDefaultChatModel(modelId)
        }
    }

    fun updateDefaultImageModel(modelId: String) {
        viewModelScope.launch {
            settingsDataStore.setDefaultImageModel(modelId)
        }
    }

    fun validateApiKey(key: String) {
        if (key.isBlank()) {
            _validationResult.value = ValidationResult.Error("API 密钥不能为空")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isValidating.value = true
            _validationResult.value = null
            try {
                val request = Request.Builder()
                    .url("https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.7-flash?key=${key.trim()}")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    _validationResult.value = ValidationResult.Success
                } else {
                    val errorMsg = response.body?.string() ?: ""
                    _validationResult.value = ValidationResult.Error("验证失败 (HTTP ${response.code}): ${errorMsg.take(100)}")
                }
            } catch (e: Exception) {
                _validationResult.value = ValidationResult.Error(e.message ?: "网络连接错误")
            } finally {
                _isValidating.value = false
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            conversationDao.deleteAllConversations()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val defaultChatModel by viewModel.defaultChatModel.collectAsStateWithLifecycle()
    val defaultImageModel by viewModel.defaultImageModel.collectAsStateWithLifecycle()

    val isValidating by viewModel.isValidating.collectAsStateWithLifecycle()
    val validationResult by viewModel.validationResult.collectAsStateWithLifecycle()

    var inputApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用设置") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. API 密钥 (Express Mode)
            SettingsSection(title = "Express Mode API 密钥") {
                OutlinedTextField(
                    value = inputApiKey,
                    onValueChange = { inputApiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (apiKeyVisible) "隐藏" else "显示"
                            )
                        }
                    },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.validateApiKey(inputApiKey) },
                        enabled = !isValidating && inputApiKey.isNotBlank()
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("验证密钥")
                    }

                    Button(
                        onClick = { viewModel.updateApiKey(inputApiKey) },
                        enabled = inputApiKey.isNotBlank() && inputApiKey != apiKey
                    ) {
                        Text("保存")
                    }
                }

                validationResult?.let { result ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (result) {
                            is ValidationResult.Success -> {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("API 密钥有效且连接正常", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium)
                            }
                            is ValidationResult.Error -> {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(result.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Text(
                    text = "用户仅需输入在 Google AI Studio 或 GCP 控制台中创建的 API 密钥即可直接调用全部 Agent Platform 模型。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // 2. 外观主题
            SettingsSection(title = "外观与主题") {
                val options = listOf("跟随系统" to "system", "浅色模式" to "light", "深色模式" to "dark")
                val selectedIndex = options.indexOfFirst { it.second == themeMode }.coerceAtLeast(0)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (label, value) ->
                        SegmentedButton(
                            selected = index == selectedIndex,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.updateThemeMode(value)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                        ) {
                            Text(label)
                        }
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material You 动态取色", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "跟随系统壁纸色调自动渲染应用界面色彩",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = dynamicColor,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.updateDynamicColor(it)
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 3. 默认模型偏好
            SettingsSection(title = "默认模型偏好") {
                // 默认聊天模型
                var chatExpanded by remember { mutableStateOf(false) }
                val currentChatModel = ModelRegistry.chatModels.find { it.id == defaultChatModel } ?: ModelRegistry.chatModels.first()

                ExposedDropdownMenuBox(
                    expanded = chatExpanded,
                    onExpandedChange = { chatExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentChatModel.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("默认聊天模型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chatExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = chatExpanded,
                        onDismissRequest = { chatExpanded = false }
                    ) {
                        ModelRegistry.chatModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.displayName) },
                                onClick = {
                                    viewModel.updateDefaultChatModel(model.id)
                                    chatExpanded = false
                                }
                            )
                        }
                    }
                }

                // 默认图片模型
                var imageExpanded by remember { mutableStateOf(false) }
                val currentImageModel = ModelRegistry.imageModels_list.find { it.id == defaultImageModel } ?: ModelRegistry.imageModels_list.first()

                ExposedDropdownMenuBox(
                    expanded = imageExpanded,
                    onExpandedChange = { imageExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentImageModel.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("默认图片模型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = imageExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = imageExpanded,
                        onDismissRequest = { imageExpanded = false }
                    ) {
                        ModelRegistry.imageModels_list.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.displayName) },
                                onClick = {
                                    viewModel.updateDefaultImageModel(model.id)
                                    imageExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // 4. 数据管理
            SettingsSection(title = "本地数据存储") {
                Button(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清除所有对话历史记录")
                }
            }

            HorizontalDivider()

            // 5. 关于
            SettingsSection(title = "关于 GCAP Client") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cloud.google.com/vertex-ai"))
                            context.startActivity(intent)
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text("GCAP Client (Android)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("版本: 1.0.0", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "专为 Google Cloud Agent Platform 设计的现代 Android 客户端，支持 Express Mode API Key 认证，具备成熟的 1:1 请求复刻与全参数 GUI 调控能力。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清除") },
            text = { Text("确定要清除所有本地对话和消息历史吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllConversations()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}
