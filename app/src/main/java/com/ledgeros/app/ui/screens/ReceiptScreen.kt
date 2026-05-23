package com.ledgeros.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ledgeros.app.ui.state.ReceiptUiState
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val scanSteps = listOf("Capture", "Scan", "Review", "Save")

@Composable
fun ReceiptScreen(
    uiState: ReceiptUiState,
    onSampleTextChange: (String) -> Unit,
    onUploadClick: () -> Unit,
    onImageSelected: (String) -> Unit,
    onRunExtractionClick: () -> Unit,
    onMerchantChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTotalChange: (String) -> Unit,
    onGstChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSaveReviewClick: () -> Unit,
) {
    val context = LocalContext.current
    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { onImageSelected(it.toString()) }
    }
    val cameraCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { captured ->
        if (captured) {
            pendingCameraUri.value?.let { onImageSelected(it.toString()) }
        }
    }

    val currentStep = when {
        uiState.receiptSaved -> 3
        uiState.reviewDraft.hasContent -> 2
        uiState.selectedImageUri != null || uiState.sampleText.isNotBlank() -> 1
        else -> 0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        item {
            ScanStepIndicator(currentStep = currentStep, steps = scanSteps)
        }

        // Success banner
        item {
            AnimatedVisibility(
                visible = uiState.receiptSaved,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200)),
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                        Column {
                            Text(
                                "Receipt saved",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "Tap Camera or Gallery to scan another.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }

        // Capture buttons
        item {
            ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Capture a receipt",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                onUploadClick()
                                val captureUri = context.createInvoiceCaptureUri()
                                pendingCameraUri.value = captureUri
                                cameraCapture.launch(captureUri)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("  Camera", style = MaterialTheme.typography.labelLarge)
                        }
                        FilledTonalButton(
                            onClick = {
                                onUploadClick()
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("  Gallery", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Image preview
        uiState.selectedImageUri?.let { imageUri ->
            item {
                ReceiptImagePreview(imageUri = imageUri)
            }
        }

        // OCR card — scan trigger or running indicator
        item {
            AnimatedVisibility(
                visible = !uiState.receiptSaved,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "OCR text",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        if (uiState.isOcrRunning) {
                            OcrRunningIndicator()
                        } else {
                            OutlinedTextField(
                                value = uiState.sampleText,
                                onValueChange = onSampleTextChange,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                label = { Text("Raw OCR or paste receipt text") },
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = onRunExtractionClick,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.sampleText.isNotBlank() || uiState.selectedImageUri != null,
                            ) {
                                Icon(
                                    Icons.Outlined.DocumentScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "  ${if (uiState.innovationMode) "Adaptive extraction" else "Deterministic extraction"}",
                                )
                            }
                        }
                    }
                }
            }
        }

        // Review draft
        if (uiState.reviewDraft.hasContent) {
            item {
                ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "  Extracted details — review & save",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        if (uiState.reviewDraft.confidence.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            ) {
                                Text(
                                    uiState.reviewDraft.confidence,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = uiState.reviewDraft.merchant,
                                onValueChange = onMerchantChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Merchant") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.reviewDraft.receiptDate,
                                onValueChange = onDateChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Date (YYYY-MM-DD)") },
                                singleLine = true,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = uiState.reviewDraft.total,
                                    onValueChange = onTotalChange,
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Total") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = uiState.reviewDraft.gst,
                                    onValueChange = onGstChange,
                                    modifier = Modifier.weight(1f),
                                    label = { Text("GST") },
                                    singleLine = true,
                                )
                            }
                            OutlinedTextField(
                                value = uiState.reviewDraft.category,
                                onValueChange = onCategoryChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Category") },
                                singleLine = true,
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onSaveReviewClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Save receipt")
                        }
                    }
                }
            }
        }

        // Status
        if (uiState.status.isNotBlank() && !uiState.receiptSaved) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = uiState.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanStepIndicator(currentStep: Int, steps: List<String>) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, label ->
                val isDone = index < currentStep
                val isActive = index == currentStep

                val dotColor = when {
                    isDone -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                val labelColor = when {
                    isDone || isActive -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(dotColor.copy(alpha = if (isActive) 0.15f else if (isDone) 0.1f else 0.06f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDone) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = dotColor,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = dotColor,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = labelColor,
                    )
                }

                if (index < steps.lastIndex) {
                    val lineProgress by animateFloatAsState(
                        targetValue = if (currentStep > index) 1f else 0f,
                        animationSpec = tween(400, easing = LinearEasing),
                        label = "step_line_$index",
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .height(2.dp)
                            .padding(bottom = 14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(lineProgress)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrRunningIndicator() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            strokeCap = StrokeCap.Round,
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Scanning receipt…",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "ML Kit is reading the text on your image.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun android.content.Context.createInvoiceCaptureUri(): Uri {
    val directory = File(cacheDir, "invoice_images").also { it.mkdirs() }
    val image = File.createTempFile("invoice_", ".jpg", directory)
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", image)
}

@Composable
private fun ReceiptImagePreview(imageUri: String) {
    val context = LocalContext.current
    val image = produceState<ImageBitmap?>(initialValue = null, imageUri) {
        value = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }
    }.value

    ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Selected receipt image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 340.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        "Loading preview…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
