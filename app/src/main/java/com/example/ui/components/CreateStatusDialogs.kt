package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.User

val STATUS_BACKGROUND_COLORS = listOf(
    0xFF0D9488L, // Teal/Emerald
    0xFF2563EBL, // Royal Blue
    0xFF7C3AEDL, // Deep Purple
    0xFFDB2777L, // Bright Magenta
    0xFFDC2626L, // Vibrant Red
    0xFFEA580CL, // Warm Orange
    0xFF059669L, // Forest Green
    0xFF4B5563L  // Slate Charcoal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTextStatusDialog(
    onDismiss: () -> Unit,
    onPostStatus: (text: String, bgHex: Long) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    val currentColor = Color(STATUS_BACKGROUND_COLORS[selectedColorIndex])

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top bar: Close & Color picker button & Post button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                selectedColorIndex = (selectedColorIndex + 1) % STATUS_BACKGROUND_COLORS.size
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = "Change background color",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (text.isNotBlank()) {
                                    onPostStatus(text, STATUS_BACKGROUND_COLORS[selectedColorIndex])
                                    onDismiss()
                                }
                            },
                            enabled = text.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = currentColor
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("post_text_status_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Post", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Centered text input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = {
                            Text(
                                "Type a status...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("text_status_input")
                    )
                }

                // Palette selector preview at bottom
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(STATUS_BACKGROUND_COLORS.indices.toList()) { index ->
                        val colorHex = STATUS_BACKGROUND_COLORS[index]
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (selectedColorIndex == index) 3.dp else 1.dp,
                                    color = if (selectedColorIndex == index) Color.White else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePhotoStatusDialog(
    initialUri: Uri? = null,
    onDismiss: () -> Unit,
    onPostPhotoStatus: (imageUri: Uri, caption: String) -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(initialUri) }
    var caption by remember { mutableStateOf("") }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Photo Status", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.1f))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Selected Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = {
                        galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Change Photo")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to select photo from gallery")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Add a caption...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedUri?.let {
                        onPostPhotoStatus(it, caption)
                        onDismiss()
                    }
                },
                enabled = selectedUri != null,
                modifier = Modifier.testTag("confirm_post_photo_status")
            ) {
                Text("Post Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatusPrivacyDialog(
    currentPrivacy: String,
    excludedIds: List<String>,
    includedIds: List<String>,
    friends: List<User>,
    onDismiss: () -> Unit,
    onSavePrivacy: (privacy: String, excludedIds: List<String>, includedIds: List<String>) -> Unit
) {
    var selectedPrivacy by remember { mutableStateOf(currentPrivacy) }
    val tempExcluded = remember { mutableStateListOf(*excludedIds.toTypedArray()) }
    val tempIncluded = remember { mutableStateListOf(*includedIds.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Status Privacy", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = "Who can see my status updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPrivacy = "MY_CONTACTS" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPrivacy == "MY_CONTACTS",
                            onClick = { selectedPrivacy = "MY_CONTACTS" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("My contacts", fontWeight = FontWeight.SemiBold)
                            Text("Share with all your contacts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPrivacy = "EXCEPT" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPrivacy == "EXCEPT",
                            onClick = { selectedPrivacy = "EXCEPT" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("My contacts except...", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (tempExcluded.isEmpty()) "No contacts excluded" else "${tempExcluded.size} excluded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedPrivacy == "EXCEPT") {
                    items(friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tempExcluded.contains(friend.uid),
                                onCheckedChange = { checked ->
                                    if (checked) tempExcluded.add(friend.uid)
                                    else tempExcluded.remove(friend.uid)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(friend.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPrivacy = "ONLY_SHARE" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPrivacy == "ONLY_SHARE",
                            onClick = { selectedPrivacy = "ONLY_SHARE" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Only share with...", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (tempIncluded.isEmpty()) "No contacts selected" else "${tempIncluded.size} included",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedPrivacy == "ONLY_SHARE") {
                    items(friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tempIncluded.contains(friend.uid),
                                onCheckedChange = { checked ->
                                    if (checked) tempIncluded.add(friend.uid)
                                    else tempIncluded.remove(friend.uid)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(friend.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSavePrivacy(selectedPrivacy, tempExcluded.toList(), tempIncluded.toList())
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
