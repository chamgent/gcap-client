package com.gcap.client.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.gcap.client.data.local.ImageStorageManager
import com.gcap.client.data.model.MessageImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePicker(
    onImagesSelected: (List<MessageImage>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageManager = remember { ImageStorageManager(context) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Multi-type Document Picker (PDF, Text, Audio, Video, Zip, Code, etc.)
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val files = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri -> storageManager.saveFileFromUri(uri) }
                }
                if (files.isNotEmpty()) {
                    onImagesSelected(files)
                }
                onDismissRequest()
            }
        } else {
            onDismissRequest()
        }
    }

    // Media Picker (Images & Videos from Gallery)
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val files = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri -> storageManager.saveFileFromUri(uri) }
                }
                if (files.isNotEmpty()) {
                    onImagesSelected(files)
                }
                onDismissRequest()
            }
        } else {
            onDismissRequest()
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            scope.launch {
                val file = withContext(Dispatchers.IO) {
                    storageManager.saveFileFromUri(tempCameraUri!!)
                }
                if (file != null) {
                    onImagesSelected(listOf(file))
                }
                onDismissRequest()
            }
        } else {
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("上传文件与多模态素材", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "支持上传图片、视频、音频、PDF 文档、代码或任意文本文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { mediaLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "相册")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("相册图片")
                }

                OutlinedButton(
                    onClick = {
                        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "拍照")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("拍照")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { mediaLauncher.launch("video/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Videocam, contentDescription = "视频")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("视频文件")
                }

                OutlinedButton(
                    onClick = { documentLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Folder, contentDescription = "全部文件")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("浏览全部文件")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
