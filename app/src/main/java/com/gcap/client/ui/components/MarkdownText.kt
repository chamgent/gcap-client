package com.gcap.client.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val components = markdownComponents(
        codeFence = { model ->
            MarkdownCodeFence(model.content, model.node) { code, language ->
                CodeBlockContainer(
                    code = code,
                    language = language,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(code))
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        Toast.makeText(context, "已复制代码", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        codeBlock = { model ->
            MarkdownCodeBlock(model.content, model.node) { code, language ->
                CodeBlockContainer(
                    code = code,
                    language = language,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(code))
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        Toast.makeText(context, "已复制代码", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        table = { model ->
            CustomMarkdownTable(
                content = model.content,
                node = model.node
            )
        }
    )

    Markdown(
        content = content,
        modifier = modifier.fillMaxWidth(),
        components = components,
        colors = markdownColor(
            text = MaterialTheme.colorScheme.onSurface,
            codeText = MaterialTheme.colorScheme.onSurfaceVariant,
            codeBackground = MaterialTheme.colorScheme.surfaceVariant
        ),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.headlineLarge,
            h2 = MaterialTheme.typography.headlineMedium,
            h3 = MaterialTheme.typography.headlineSmall,
            h4 = MaterialTheme.typography.titleLarge,
            h5 = MaterialTheme.typography.titleMedium,
            h6 = MaterialTheme.typography.titleSmall,
            text = MaterialTheme.typography.bodyLarge,
            code = MaterialTheme.typography.bodyMedium
        )
    )
}

@Composable
private fun CustomMarkdownTable(
    content: String,
    node: ASTNode,
    modifier: Modifier = Modifier
) {
    val headerNode = node.children.find { it.type == GFMElementTypes.HEADER }
    val rowNodes = node.children.filter { it.type == GFMElementTypes.ROW }

    val headerCells = headerNode?.children?.filter { it.type == GFMTokenTypes.CELL }?.map {
        it.getTextInNode(content).toString().trim()
    } ?: emptyList()

    val dataRows = rowNodes.map { rowNode ->
        rowNode.children.filter { it.type == GFMTokenTypes.CELL }.map {
            it.getTextInNode(content).toString().trim()
        }
    }

    if (headerCells.isEmpty() && dataRows.isEmpty()) return

    val columnCount = maxOf(headerCells.size, dataRows.maxOfOrNull { it.size } ?: 0)
    if (columnCount == 0) return

    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                // Header row
                if (headerCells.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        for (colIndex in 0 until columnCount) {
                            val text = headerCells.getOrNull(colIndex) ?: ""
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 110.dp)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                            if (colIndex < columnCount - 1) {
                                VerticalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                // Data rows
                dataRows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        for (colIndex in 0 until columnCount) {
                            val text = row.getOrNull(colIndex) ?: ""
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 110.dp)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                            if (colIndex < columnCount - 1) {
                                VerticalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }
                        }
                    }
                    if (rowIndex < dataRows.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockContainer(
    code: String,
    language: String?,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayLang = language?.trim()?.uppercase()?.ifEmpty { "CODE" } ?: "CODE"
    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        Column {
            // Header: Language + Copy Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayLang,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制代码",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Code content with horizontal scroll
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
