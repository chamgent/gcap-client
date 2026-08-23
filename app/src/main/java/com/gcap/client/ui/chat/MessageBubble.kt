package com.gcap.client.ui.chat

import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.gcap.client.data.model.ChatMessage
import com.gcap.client.data.model.MessageImage
import com.gcap.client.data.model.MessageRole
import com.gcap.client.ui.components.MarkdownText
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onEditModelMessage: ((messageId: String, newText: String) -> Unit)? = null,
    onEditAndResendUserMessage: ((messageId: String, newText: String) -> Unit)? = null
) {
    if (message.role == MessageRole.USER) {
        UserMessageBubble(
            message = message,
            modifier = modifier,
            onEditAndResendUserMessage = onEditAndResendUserMessage,
            onEditSaveOnly = onEditModelMessage
        )
    } else {
        ModelMessageView(
            message = message,
            modifier = modifier,
            onEditModelMessage = onEditModelMessage,
            onRegenerate = {
                onEditAndResendUserMessage?.invoke(message.id, message.textContent)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onEditAndResendUserMessage: ((messageId: String, newText: String) -> Unit)? = null,
    onEditSaveOnly: ((messageId: String, newText: String) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        Card(
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Attached media & files
                if (message.images.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        message.images.forEach { file ->
                            AttachmentChip(file = file)
                        }
                    }
                }

                SelectionContainer {
                    Text(
                        text = message.textContent,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Action bar below bubble
        Row(
            modifier = Modifier.padding(top = 2.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    clipboardManager.setText(AnnotatedString(message.textContent))
                    Toast.makeText(context, "已复制消息到剪贴板", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "复制",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }

            if (onEditAndResendUserMessage != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onEditAndResendUserMessage(message.id, message.textContent)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "原位重发",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditMessageDialog(
            message = message,
            onDismiss = { showEditDialog = false },
            onSaveOnly = { newText ->
                onEditSaveOnly?.invoke(message.id, newText)
            },
            onResendInPlace = if (onEditAndResendUserMessage != null) {
                { newText -> onEditAndResendUserMessage(message.id, newText) }
            } else null
        )
    }
}

@Composable
fun ModelMessageView(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onEditModelMessage: ((messageId: String, newText: String) -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var showThinking by remember(message.id) { mutableStateOf(message.isStreaming && message.thinkingContent.isNotBlank()) }
    var showToolCalls by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<MessageImage?>(null) }
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    androidx.compose.runtime.LaunchedEffect(message.isStreaming, message.thinkingContent.isNotEmpty()) {
        if (message.isStreaming && message.thinkingContent.isNotEmpty() && !showThinking) {
            showThinking = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Model Header (Clean icon + Model name)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = message.modelName.ifBlank { "Gemini" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Thinking Process (Collapsible pill like DeepSeek/ChatGPT)
        if (message.thinkingContent.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { showThinking = !showThinking }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "思考过程 (${message.thinkingContent.length} 字)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    AnimatedVisibility(visible = showThinking) {
                        SelectionContainer {
                            Text(
                                text = message.thinkingContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Tool Calls & Grounding
        if (message.toolCallContent.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { showToolCalls = !showToolCalls }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🛠️ 工具调用与检索来源",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (showToolCalls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    AnimatedVisibility(visible = showToolCalls) {
                        SelectionContainer {
                            MarkdownText(
                                content = message.toolCallContent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Main Markdown Text (Full-width, clean typography)
        if (message.textContent.isNotEmpty()) {
            SelectionContainer {
                MarkdownText(
                    content = message.textContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Attached/Generated Images & Media
        if (message.images.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.images.forEach { image ->
                    val imageModel = remember(image.base64Data, image.uri) {
                        when {
                            image.uri != null -> image.uri
                            image.base64Data != null -> {
                                try {
                                    val clean = image.base64Data.substringAfter("base64,").trim()
                                    Base64.decode(clean, Base64.DEFAULT)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            else -> null
                        }
                    }

                    if (imageModel != null && image.mimeType.startsWith("image/")) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { fullScreenImage = image },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AttachmentChip(file = image)
                    }
                }
            }
        }

        // Streaming indicator
        if (message.isStreaming) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StreamingIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "生成中...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Error message
        if (message.error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "错误",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = message.error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Action bar & Token Metrics (Like DeepSeek/ChatGPT footer)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        clipboardManager.setText(AnnotatedString(message.textContent))
                        Toast.makeText(context, "已复制消息到剪贴板", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                if (onRegenerate != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onRegenerate()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "重新生成",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Timestamp and Tokens metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (message.promptTokens != null || message.candidatesTokens != null) {
                    Text(
                        text = "↑${message.promptTokens ?: 0} ↓${message.candidatesTokens ?: 0} tok",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }

    if (showEditDialog) {
        EditMessageDialog(
            message = message,
            onDismiss = { showEditDialog = false },
            onSaveOnly = { newText ->
                onEditModelMessage?.invoke(message.id, newText)
            }
        )
    }

    fullScreenImage?.let { image ->
        FullScreenImageDialog(
            image = image,
            onDismiss = { fullScreenImage = null }
        )
    }
}

@Composable
fun AttachmentChip(file: MessageImage) {
    val context = LocalContext.current
    val (icon, label) = when {
        file.mimeType.startsWith("image/") -> Pair(Icons.Outlined.AutoAwesome, file.name ?: "图片")
        file.mimeType.startsWith("video/") -> Pair(Icons.Outlined.Videocam, file.name ?: "视频文件")
        file.mimeType.startsWith("audio/") -> Pair(Icons.Outlined.Audiotrack, file.name ?: "音频文件")
        else -> Pair(Icons.Outlined.Description, file.name ?: "文档文件")
    }

    AssistChip(
        onClick = {
            file.uri?.let { uriStr ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(uriStr), file.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "无法打开此文件", Toast.LENGTH_SHORT).show()
                }
            }
        },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        label = {
            Text(
                text = "${file.name ?: label} ${if (file.size > 0) formatFileSize(file.size) else ""}".trim(),
                maxLines = 1
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    )
}

@Composable
fun EditMessageDialog(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onSaveOnly: (String) -> Unit,
    onResendInPlace: ((String) -> Unit)? = null
) {
    var text by remember { mutableStateOf(message.textContent) }
    val isUser = message.role == MessageRole.USER
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isUser) "编辑用户消息" else "编辑模型回复")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isUser) {
                    Text(
                        text = "编辑后将直接覆盖当前模型消息内容，不影响后续消息。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "「原位重发」将以此内容重新生成后续回复；「仅保存」仅修改当前文字记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = { Text("输入消息内容...") }
                )
            }
        },
        confirmButton = {
            if (isUser && onResendInPlace != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onResendInPlace(text)
                        onDismiss()
                    },
                    enabled = text.isNotBlank()
                ) {
                    Text("原位重发")
                }
            } else {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onSaveOnly(text)
                        onDismiss()
                    },
                    enabled = text.isNotBlank()
                ) {
                    Text("保存覆盖")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isUser) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onSaveOnly(text)
                            onDismiss()
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Text("仅保存")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
fun FullScreenImageDialog(image: MessageImage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val imageModel = remember(image.base64Data, image.uri) {
        when {
            image.uri != null -> image.uri
            image.base64Data != null -> {
                try {
                    val clean = image.base64Data.substringAfter("base64,").trim()
                    Base64.decode(clean, Base64.DEFAULT)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = "大图预览",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )
            }

            // Top action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }

                FilledTonalButton(
                    onClick = {
                        saveImageToGallery(context, image)
                        Toast.makeText(context, "图片已保存至相册", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存至相册")
                }
            }
        }
    }
}

fun saveImageToGallery(context: Context, image: MessageImage) {
    try {
        val filename = "GCAP_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GCAP")
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            val bytes = when {
                image.uri != null -> {
                    val uri = Uri.parse(image.uri)
                    if (uri.scheme == "file") {
                        java.io.File(uri.path ?: "").readBytes()
                    } else {
                        resolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                }
                image.base64Data != null -> {
                    val clean = image.base64Data.substringAfter("base64,").trim()
                    Base64.decode(clean, Base64.DEFAULT)
                }
                else -> null
            }
            if (bytes != null) {
                resolver.openOutputStream(imageUri)?.use { it.write(bytes) }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun StreamingIndicator(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatTimestamp(time: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(time))
}
