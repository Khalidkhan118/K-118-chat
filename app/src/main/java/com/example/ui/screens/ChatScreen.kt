package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.ChatMessage
import com.example.model.User
import com.example.ui.ChatViewModel
import com.example.ui.components.FullScreenImageViewerDialog
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.components.TypingIndicatorBubble
import com.example.ui.components.UserAvatar
import com.example.ui.theme.EmeraldOnline
import com.example.ui.util.CameraUtils
import com.example.ui.util.TimeUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    recipientUser: User,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val messages by viewModel.activeChatMessages.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val isOtherUserTyping by viewModel.isOtherUserTyping.collectAsState()
    val otherUserTypingName by viewModel.otherUserTypingName.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val replyingToMessage by viewModel.replyingToMessage.collectAsState()

    val currentRecipient = friends.firstOrNull { it.uid == recipientUser.uid } ?: recipientUser
    val isBlockedByMe = currentUser?.blockedUserIds?.contains(currentRecipient.uid) == true

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var selectedFullViewMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteForEveryoneConfirmDialog by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }

    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPreviewDialog by remember { mutableStateOf(false) }

    var selectedGalleryUri by remember { mutableStateOf<Uri?>(null) }
    var showGalleryPreviewDialog by remember { mutableStateOf(false) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedGalleryUri = uri
            showGalleryPreviewDialog = true
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess && tempPhotoUri != null) {
            capturedPhotoUri = tempPhotoUri
            showPhotoPreviewDialog = true
        } else {
            tempPhotoFile?.delete()
            tempPhotoFile = null
            tempPhotoUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val (file, uri) = CameraUtils.createImageUri(context)
            tempPhotoFile = file
            tempPhotoUri = uri
            try {
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                val (sampleFile, sampleUri) = CameraUtils.createSampleCameraPhoto(context)
                tempPhotoFile = sampleFile
                capturedPhotoUri = sampleUri
                showPhotoPreviewDialog = true
            }
        }
    }

    val launchCamera: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val (file, uri) = CameraUtils.createImageUri(context)
            tempPhotoFile = file
            tempPhotoUri = uri
            try {
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                val (sampleFile, sampleUri) = CameraUtils.createSampleCameraPhoto(context)
                tempPhotoFile = sampleFile
                capturedPhotoUri = sampleUri
                showPhotoPreviewDialog = true
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(messages.size, isOtherUserTyping) {
        val totalCount = messages.size + (if (isOtherUserTyping) 1 else 0)
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    // Wallpaper background color styling
    val wallpaperColor = when (currentUser?.chatWallpaper) {
        "emerald_doodle" -> Color(0xFF0B141A) // WhatsApp dark default
        "midnight" -> Color(0xFF0F172A)
        "slate" -> Color(0xFF1E293B)
        "sunset" -> Color(0xFF1C1326)
        else -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Could view user detail */ }
                    ) {
                        UserAvatar(
                            photoUrl = currentRecipient.photoUrl,
                            displayName = currentRecipient.displayName,
                            size = 40.dp,
                            isOnline = currentRecipient.isOnline,
                            showOnlineBadge = true
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = currentRecipient.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOtherUserTyping) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldOnline)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "typing...",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldOnline,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                } else {
                                    Text(
                                        text = if (currentRecipient.isOnline) "online" else TimeUtils.formatLastSeen(false, currentRecipient.lastSeenTimestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (currentRecipient.isOnline) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startCall(currentRecipient, false) }) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.startCall(currentRecipient, true) }) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isBlockedByMe) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "You blocked this contact. Unblock to send messages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    // Quoted Reply Preview banner
                    replyingToMessage?.let { reply ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (reply.senderId == (currentUser?.uid ?: "")) "You" else currentRecipient.displayName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = reply.text.ifEmpty { if (reply.mediaUrl.isNotBlank()) "📷 Photo" else "" },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setReplyingToMessage(null) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = launchCamera,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Take photo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Pick image",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                viewModel.onInputTextChanged(it)
                            },
                            placeholder = { Text("Message...") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        val canSend = inputText.isNotBlank()
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (canSend) EmeraldOnline else MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = wallpaperColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (messages.isEmpty() && !isOtherUserTyping) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            UserAvatar(
                                photoUrl = currentRecipient.photoUrl,
                                displayName = currentRecipient.displayName,
                                size = 72.dp,
                                isOnline = currentRecipient.isOnline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentRecipient.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Start a 1-to-1 conversation with ${currentRecipient.displayName}. Messages are synchronized in real-time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            val isFromMe = message.senderId == (currentUser?.uid ?: "")

                            WhatsAppMessageBubble(
                                message = message,
                                isFromMe = isFromMe,
                                recipientUserId = currentRecipient.uid,
                                senderName = if (isFromMe) (currentUser?.displayName ?: "Me") else currentRecipient.displayName,
                                currentUserId = currentUser?.uid ?: "",
                                onImageClick = { selectedFullViewMessage = it },
                                onMessageLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedMessageForActions = it
                                },
                                onReactionClick = { emoji ->
                                    viewModel.toggleReaction(message.id, emoji)
                                }
                            )
                        }

                        item(key = "typing_indicator_item") {
                            if (isOtherUserTyping) {
                                TypingIndicatorBubble(
                                    userName = otherUserTypingName ?: currentRecipient.displayName,
                                    photoUrl = currentRecipient.photoUrl,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // LONG-PRESS MESSAGE ACTION BOTTOM SHEET / DIALOG
    // -------------------------------------------------------------
    selectedMessageForActions?.let { msg ->
        val isSenderMe = msg.senderId == (currentUser?.uid ?: "")
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedMessageForActions = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Quick emoji reaction bar at top of sheet
                QuickEmojiBar(
                    onEmojiSelected = { emoji ->
                        viewModel.toggleReaction(msg.id, emoji)
                        selectedMessageForActions = null
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Copy
                if (msg.text.isNotBlank() && !msg.isDeletedForEveryone) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                selectedMessageForActions = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Copy", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // Reply
                if (!msg.isDeletedForEveryone) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setReplyingToMessage(msg)
                                selectedMessageForActions = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Reply", style = MaterialTheme.typography.bodyLarge)
                    }

                    // Forward
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showForwardDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Forward", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDeleteOptionsDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Delete message", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // -------------------------------------------------------------
    // DELETE OPTIONS DIALOG ("Delete for me" vs "Delete for everyone")
    // -------------------------------------------------------------
    if (showDeleteOptionsDialog && selectedMessageForActions != null) {
        val targetMsg = selectedMessageForActions!!
        val isSenderMe = targetMsg.senderId == (currentUser?.uid ?: "")

        AlertDialog(
            onDismissRequest = {
                showDeleteOptionsDialog = false
                selectedMessageForActions = null
            },
            title = { Text("Delete message?") },
            text = {
                Column {
                    Text("Choose how you want to delete this message.")
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isSenderMe && !targetMsg.isDeletedForEveryone) {
                        TextButton(
                            onClick = {
                                showDeleteOptionsDialog = false
                                showDeleteForEveryoneConfirmDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete for everyone", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.deleteMessageForMe(targetMsg.id)
                            showDeleteOptionsDialog = false
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete for me", color = MaterialTheme.colorScheme.primary)
                    }

                    TextButton(
                        onClick = {
                            showDeleteOptionsDialog = false
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Confirmation before Delete for everyone
    if (showDeleteForEveryoneConfirmDialog && selectedMessageForActions != null) {
        val targetMsg = selectedMessageForActions!!
        AlertDialog(
            onDismissRequest = {
                showDeleteForEveryoneConfirmDialog = false
                selectedMessageForActions = null
            },
            title = { Text("Delete for everyone?") },
            text = { Text("This message will be deleted for everyone in this chat.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMessageForEveryone(targetMsg.id)
                        showDeleteForEveryoneConfirmDialog = false
                        selectedMessageForActions = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete for everyone")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteForEveryoneConfirmDialog = false
                    selectedMessageForActions = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Forward Dialog
    if (showForwardDialog && selectedMessageForActions != null) {
        val targetMsg = selectedMessageForActions!!
        AlertDialog(
            onDismissRequest = {
                showForwardDialog = false
                selectedMessageForActions = null
            },
            title = { Text("Forward to...") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(friends, key = { it.uid }) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.forwardMessage(targetMsg, friend)
                                    showForwardDialog = false
                                    selectedMessageForActions = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(photoUrl = friend.photoUrl, displayName = friend.displayName, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showForwardDialog = false
                    selectedMessageForActions = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Media preview dialogs
    if (showPhotoPreviewDialog && capturedPhotoUri != null) {
        PhotoPreviewDialog(
            photoUri = capturedPhotoUri!!,
            recipientName = currentRecipient.displayName,
            isFromGallery = false,
            isUploading = isUploadingImage,
            uploadProgress = uploadProgress,
            onSendPhoto = { caption ->
                viewModel.sendGalleryImage(
                    imageUri = capturedPhotoUri!!,
                    caption = caption,
                    onComplete = {
                        showPhotoPreviewDialog = false
                        capturedPhotoUri = null
                        tempPhotoFile = null
                        tempPhotoUri = null
                    }
                )
            },
            onRetake = {
                showPhotoPreviewDialog = false
                capturedPhotoUri = null
                tempPhotoFile?.delete()
                tempPhotoFile = null
                tempPhotoUri = null
                launchCamera()
            },
            onDismiss = {
                showPhotoPreviewDialog = false
                capturedPhotoUri = null
                tempPhotoFile?.delete()
                tempPhotoFile = null
                tempPhotoUri = null
            }
        )
    }

    if (showGalleryPreviewDialog && selectedGalleryUri != null) {
        PhotoPreviewDialog(
            photoUri = selectedGalleryUri!!,
            recipientName = currentRecipient.displayName,
            isFromGallery = true,
            isUploading = isUploadingImage,
            uploadProgress = uploadProgress,
            onSendPhoto = { caption ->
                viewModel.sendGalleryImage(
                    imageUri = selectedGalleryUri!!,
                    caption = caption,
                    onComplete = {
                        showGalleryPreviewDialog = false
                        selectedGalleryUri = null
                    }
                )
            },
            onRetake = {
                galleryPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = {
                showGalleryPreviewDialog = false
                selectedGalleryUri = null
            }
        )
    }

    if (selectedFullViewMessage != null) {
        val isSenderMe = selectedFullViewMessage!!.senderId == (currentUser?.uid ?: "")
        val sName = if (isSenderMe) (currentUser?.displayName ?: "Me") else currentRecipient.displayName
        FullScreenImageViewerDialog(
            message = selectedFullViewMessage!!,
            senderName = sName,
            onDismiss = { selectedFullViewMessage = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WhatsAppMessageBubble(
    message: ChatMessage,
    isFromMe: Boolean,
    recipientUserId: String,
    senderName: String,
    currentUserId: String = "",
    onImageClick: (ChatMessage) -> Unit = {},
    onMessageLongClick: (ChatMessage) -> Unit = {},
    onReactionClick: (String) -> Unit = {}
) {
    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    // WhatsApp style container colors
    val containerColor = if (message.isDeletedForEveryone) {
        if (isFromMe) Color(0xFF005C4B).copy(alpha = 0.5f) else Color(0xFF202C33).copy(alpha = 0.5f)
    } else if (isFromMe) {
        Color(0xFF005C4B) // WhatsApp dark outgoing bubble
    } else {
        Color(0xFF202C33) // WhatsApp dark incoming bubble
    }

    val textColor = Color.White
    val timeColor = Color.White.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_bubble_${message.id}"),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(containerColor)
                .combinedClickable(
                    onClick = {
                        if (message.mediaUrl.isNotBlank() && !message.isDeletedForEveryone) {
                            onImageClick(message)
                        }
                    },
                    onLongClick = { onMessageLongClick(message) }
                )
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Column {
                // Quoted reply snippet inside bubble
                if (!message.isDeletedForEveryone && message.replyToText != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(EmeraldOnline)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = message.replyToSenderName ?: "Contact",
                                    color = EmeraldOnline,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = message.replyToText,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // If deleted for everyone
                if (message.isDeletedForEveryone) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotInterested,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFromMe) "You deleted this message" else "This message was deleted",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Photo Media
                    if (message.mediaUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .combinedClickable(
                                    onClick = { onImageClick(message) },
                                    onLongClick = { onMessageLongClick(message) }
                                )
                                .testTag("message_image_${message.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(message.mediaUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Photo from $senderName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth(),
                                loading = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp,
                                        color = EmeraldOnline
                                    )
                                }
                            )
                        }

                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Text Content
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = textColor,
                            modifier = if (message.mediaUrl.isNotBlank()) Modifier.padding(horizontal = 4.dp) else Modifier
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Time and Double-Ticks Read Receipts
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimeUtils.formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = timeColor
                    )

                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val isRead = message.isRead || message.readBy.contains(recipientUserId)
                        val isDelivered = isRead || message.deliveredTo.contains(recipientUserId)

                        if (isRead) {
                            // Double Blue Ticks (Read)
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = Color(0xFF53BDEB), // WhatsApp blue ticks
                                modifier = Modifier.size(15.dp)
                            )
                        } else if (isDelivered) {
                            // Double Gray Ticks (Delivered)
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = timeColor,
                                modifier = Modifier.size(15.dp)
                            )
                        } else {
                            // Single Tick (Sent)
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = timeColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Reaction Badges Row
        if (message.reactions.isNotEmpty() && !message.isDeletedForEveryone) {
            val groupedReactions = message.reactions.entries.groupBy({ it.value }, { it.key })

            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = if (isFromMe) 0.dp else 4.dp, end = if (isFromMe) 4.dp else 0.dp)
                    .testTag("message_reactions_${message.id}"),
                horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                groupedReactions.forEach { (emoji, userIds) ->
                    val hasMyReaction = userIds.contains(currentUserId)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF202C33),
                        border = BorderStroke(
                            1.dp,
                            if (hasMyReaction) EmeraldOnline else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clickable { onReactionClick(emoji) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            if (userIds.size > 1) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${userIds.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickEmojiBar(
    onEmojiSelected: (String) -> Unit
) {
    val emojis = listOf("❤️", "👍", "😂", "🔥", "👏", "🎉", "🚀", "👋", "🙏", "😮")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(emojis) { emoji ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onEmojiSelected(emoji) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("quick_emoji_$emoji"),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 16.sp)
            }
        }
    }
}
