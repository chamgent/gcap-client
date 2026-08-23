package com.gcap.client.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gcap.client.data.local.ConversationDao
import com.gcap.client.data.local.CustomModelEntity
import com.gcap.client.data.local.CustomProviderEntity
import com.gcap.client.data.local.SettingsDataStore
import com.gcap.client.data.model.ModelRegistry
import com.gcap.client.data.repository.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import javax.inject.Inject

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val repository: GeminiRepository,
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

    val customProviders: StateFlow<List<CustomProviderEntity>> = repository.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customModels: StateFlow<List<CustomModelEntity>> = repository.getAllCustomModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isValidating = MutableStateFlow(false)
    val isValidating = _isValidating.asStateFlow()

    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult = _validationResult.asStateFlow()

    private val _probingProviderIds = MutableStateFlow<Set<String>>(emptySet())
    val probingProviderIds = _probingProviderIds.asStateFlow()

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

    fun saveCustomProvider(
        id: String?,
        name: String,
        baseUrl: String,
        apiKey: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val providerId = id ?: UUID.randomUUID().toString()
            val entity = CustomProviderEntity(
                id = providerId,
                name = name.trim(),
                baseUrl = baseUrl.trim(),
                apiKey = apiKey.trim()
            )
            repository.upsertProvider(entity)

            // Auto probe models
            _probingProviderIds.value = _probingProviderIds.value + providerId
            val probeResult = repository.probeAndSaveModels(entity)
            _probingProviderIds.value = _probingProviderIds.value - providerId

            if (probeResult.isSuccess) {
                val count = probeResult.getOrDefault(emptyList()).size
                onResult(true, "服务商已保存，自动探测到 $count 款可用模型！")
            } else {
                onResult(false, "服务商已保存，但探测模型失败: ${probeResult.exceptionOrNull()?.message ?: "连接失败"}")
            }
        }
    }

    fun refreshProviderModels(provider: CustomProviderEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _probingProviderIds.value = _probingProviderIds.value + provider.id
            val result = repository.probeAndSaveModels(provider)
            _probingProviderIds.value = _probingProviderIds.value - provider.id

            if (result.isSuccess) {
                val count = result.getOrDefault(emptyList()).size
                onResult(true, "刷新成功，探测到 $count 款模型！")
            } else {
                onResult(false, "探测失败: ${result.exceptionOrNull()?.message ?: "网络错误"}")
            }
        }
    }

    fun deleteCustomProvider(providerId: String) {
        viewModelScope.launch {
            repository.deleteProvider(providerId)
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            conversationDao.deleteAllConversations()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val customProviders by viewModel.customProviders.collectAsStateWithLifecycle()
    val customModels by viewModel.customModels.collectAsStateWithLifecycle()
    val probingIds by viewModel.probingProviderIds.collectAsStateWithLifecycle()

    val isValidating by viewModel.isValidating.collectAsStateWithLifecycle()
    val validationResult by viewModel.validationResult.collectAsStateWithLifecycle()

    var inputApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var providerDialogTarget by remember { mutableStateOf<CustomProviderEntity?>(null) }
    var showAddProviderDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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
            // 1. Google Express Mode API 密钥
            SettingsSection(title = "Google Express Mode API 密钥") {
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
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.validateApiKey(inputApiKey)
                        },
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
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.updateApiKey(inputApiKey)
                            Toast.makeText(context, "已保存 Google API 密钥", Toast.LENGTH_SHORT).show()
                        },
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
                    text = "用于调用 Google Vertex AI / Agent Platform 的 Gemini 官方模型序列。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // 2. 自定义 OpenAI 兼容服务商
            SettingsSection(title = "自定义 OpenAI 兼容服务商") {
                Text(
                    text = "支持接入 DeepSeek、OpenRouter、SiliconFlow、Ollama、Groq 等任意 OpenAI 协议服务商，自动探测可用模型与视觉/推理能力并加入顶部模型列表。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (customProviders.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        customProviders.forEach { provider ->
                            val providerModels = customModels.filter { it.providerId == provider.id }
                            val isProbing = provider.id in probingIds

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = provider.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "${providerModels.size} 个模型",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    providerDialogTarget = provider
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.deleteCustomProvider(provider.id)
                                                    Toast.makeText(context, "已删除服务商: ${provider.name}", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "删除",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "URL: ${provider.baseUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (providerModels.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            providerModels.take(8).forEach { m ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surface
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Text(text = m.displayName, style = MaterialTheme.typography.labelSmall)
                                                        if (m.supportsVision) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Visibility,
                                                                contentDescription = "视觉",
                                                                modifier = Modifier.size(10.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        if (m.supportsReasoning) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Psychology,
                                                                contentDescription = "推理",
                                                                modifier = Modifier.size(11.dp),
                                                                tint = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (providerModels.size > 8) {
                                                Text(
                                                    text = "+${providerModels.size - 8} 更多...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.align(Alignment.CenterVertically)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.refreshProviderModels(provider) { success, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isProbing,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isProbing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(if (isProbing) "正在探测模型..." else "重新探测可用模型")
                                    }
                                }
                            }
                        }
                    }
                }

                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAddProviderDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("添加自定义 OpenAI 服务商")
                }
            }

            HorizontalDivider()

            // 3. 外观主题
            SettingsSection(title = "外观与主题") {
                val options = listOf("跟随系统" to "system", "浅色模式" to "light", "深色模式" to "dark")
                val selectedIndex = options.indexOfFirst { it.second == themeMode }.coerceAtLeast(0)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (label, value) ->
                        SegmentedButton(
                            selected = index == selectedIndex,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateDynamicColor(it)
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // 4. 默认模型偏好
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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

            // 5. 本地数据管理
            SettingsSection(title = "本地数据存储") {
                Button(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清除所有对话历史记录")
                }
            }

            HorizontalDivider()

            // 6. 关于
            SettingsSection(title = "关于 GCAP Client") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/chamgent/gcap-client"))
                            context.startActivity(intent)
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text("GCAP Client (Android)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("版本: 1.1.0", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "支持 Google Vertex AI Express Mode 与任意 OpenAI 兼容服务商，具备成熟的多模态交互、模型自动探测、思维链折叠与全参数 GUI 调控能力。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAddProviderDialog || providerDialogTarget != null) {
        val target = providerDialogTarget
        ProviderEditDialog(
            initialProvider = target,
            onDismiss = {
                showAddProviderDialog = false
                providerDialogTarget = null
            },
            onSave = { name, baseUrl, apiKey ->
                viewModel.saveCustomProvider(
                    id = target?.id,
                    name = name,
                    baseUrl = baseUrl,
                    apiKey = apiKey
                ) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                showAddProviderDialog = false
                providerDialogTarget = null
            }
        )
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
                        Toast.makeText(context, "所有对话记录已清除", Toast.LENGTH_SHORT).show()
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
fun ProviderEditDialog(
    initialProvider: CustomProviderEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, apiKey: String) -> Unit
) {
    var name by remember { mutableStateOf(initialProvider?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initialProvider?.baseUrl ?: "https://") }
    var apiKey by remember { mutableStateOf(initialProvider?.apiKey ?: "") }
    var apiKeyVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProvider == null) "添加 OpenAI 兼容服务商" else "编辑服务商") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("提供商名称") },
                    placeholder = { Text("如: DeepSeek, OpenRouter, SiliconFlow") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("API Base URL") },
                    placeholder = { Text("https://api.deepseek.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (apiKeyVisible) "隐藏" else "显示"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "保存后将自动连接并探测服务商支持的全部模型，并自动分析标注其视觉与推理能力。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, baseUrl, apiKey) },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank()
            ) {
                Text("探测并保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
