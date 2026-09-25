package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StatusItem
import com.example.model.User
import com.example.model.UserStatusGroup
import com.example.ui.ChatViewModel
import com.example.ui.components.CreatePhotoStatusDialog
import com.example.ui.components.CreateTextStatusDialog
import com.example.ui.components.StatusPrivacyDialog
import com.example.ui.components.StatusViewerScreen
import com.example.ui.components.UserAvatar
import com.example.ui.theme.EmeraldOnline
import com.example.ui.util.TimeUtils

@Composable
fun StatusTabContent(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val myStatuses by viewModel.myStatuses.collectAsState()
    val friendsStatusGroups by viewModel.friendsStatusGroups.collectAsState()

    var showCreateTextStatusDialog by remember { mutableStateOf(false) }
    var showCreatePhotoStatusDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Active viewer state
    var viewingStatusGroup by remember { mutableStateOf<UserStatusGroup?>(null) }
    var isViewingMyStatus by remember { mutableStateOf(false) }

    var isViewedSectionExpanded by remember { mutableStateOf(true) }
    var showStatusMenu by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            showCreatePhotoStatusDialog = true
        }
    }

    val me = currentUser ?: return

    val unviewedGroups = friendsStatusGroups.filter { it.hasUnviewed }
    val viewedGroups = friendsStatusGroups.filter { !it.hasUnviewed }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // -------------------------------------------------------------
            // PRIVACY HEADER BANNER
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    IconButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.testTag("status_privacy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Status Privacy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // MY STATUS ROW
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStatuses.isNotEmpty()) {
                                viewingStatusGroup = UserStatusGroup(
                                    user = me,
                                    statuses = myStatuses,
                                    hasUnviewed = false,
                                    lastUpdated = myStatuses.maxOfOrNull { it.timestamp } ?: 0L
                                )
                                isViewingMyStatus = true
                            } else {
                                showCreateTextStatusDialog = true
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("my_status_item"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        UserAvatar(
                            photoUrl = me.photoUrl,
                            displayName = me.displayName,
                            size = 52.dp,
                            showOnlineBadge = false,
                            hasStatus = myStatuses.isNotEmpty(),
                            hasUnviewedStatus = true,
                            statusCount = myStatuses.size
                        )

                        if (myStatuses.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldOnline),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add status",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My status",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (myStatuses.isNotEmpty()) {
                                val latest = myStatuses.maxByOrNull { it.timestamp }
                                "${TimeUtils.formatStatusUploadTime(latest?.timestamp ?: 0L)} • ${myStatuses.size} update${if (myStatuses.size > 1) "s" else ""}"
                            } else {
                                "Tap to add status update"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick buttons: pencil (text) & camera (photo)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showCreateTextStatusDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("create_text_status_quick")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Add text status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("create_photo_status_quick")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Add photo status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }

            // -------------------------------------------------------------
            // RECENT UPDATES (UNVIEWED)
            // -------------------------------------------------------------
            if (unviewedGroups.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent updates",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(unviewedGroups, key = { it.user.uid }) { group ->
                    StatusGroupRow(
                        group = group,
                        onClick = {
                            viewingStatusGroup = group
                            isViewingMyStatus = false
                        }
                    )
                }
            }

            // -------------------------------------------------------------
            // VIEWED UPDATES
            // -------------------------------------------------------------
            if (viewedGroups.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isViewedSectionExpanded = !isViewedSectionExpanded }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Viewed updates",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Icon(
                            imageVector = if (isViewedSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isViewedSectionExpanded) {
                    items(viewedGroups, key = { it.user.uid }) { group ->
                        StatusGroupRow(
                            group = group,
                            onClick = {
                                viewingStatusGroup = group
                                isViewingMyStatus = false
                            }
                        )
                    }
                }
            }

            // Empty state if no friends have statuses
            if (friendsStatusGroups.isEmpty() && myStatuses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 32.dp, end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Status Updates Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Share text thoughts or photos that disappear after 24 hours.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Buttons (Pencil text & Camera photo)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { showCreateTextStatusDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("fab_text_status")
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Add Text Status", modifier = Modifier.size(20.dp))
            }

            FloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                containerColor = EmeraldOnline,
                contentColor = Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_photo_status")
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Add Photo Status", modifier = Modifier.size(26.dp))
            }
        }
    }

    // Fullscreen Status Viewing Screen
    viewingStatusGroup?.let { group ->
        StatusViewerScreen(
            user = group.user,
            statuses = group.statuses,
            isMyStatus = isViewingMyStatus,
            currentUserId = me.uid,
            onStatusViewed = { statusId ->
                viewModel.markStatusViewed(statusId)
            },
            onDeleteStatus = { statusId ->
                viewModel.deleteStatus(statusId)
            },
            onReplyToStatus = { recipientUser, reply ->
                viewModel.openChatWith(recipientUser)
                viewModel.sendMessage("Replying to status: \"$reply\"")
            },
            onDismiss = {
                viewingStatusGroup = null
                isViewingMyStatus = false
            }
        )
    }

    // Dialogs
    if (showCreateTextStatusDialog) {
        CreateTextStatusDialog(
            onDismiss = { showCreateTextStatusDialog = false },
            onPostStatus = { text, bgHex ->
                viewModel.postTextStatus(text, bgHex)
            }
        )
    }

    if (showCreatePhotoStatusDialog) {
        CreatePhotoStatusDialog(
            initialUri = selectedPhotoUri,
            onDismiss = {
                showCreatePhotoStatusDialog = false
                selectedPhotoUri = null
            },
            onPostPhotoStatus = { uri, caption ->
                viewModel.postImageStatus(uri, caption)
            }
        )
    }

    if (showPrivacyDialog) {
        StatusPrivacyDialog(
            currentPrivacy = me.statusPrivacy,
            excludedIds = me.statusPrivacyExcludedIds,
            includedIds = me.statusPrivacyIncludedIds,
            friends = friends,
            onDismiss = { showPrivacyDialog = false },
            onSavePrivacy = { privacy, excluded, included ->
                viewModel.updateStatusPrivacy(privacy, excluded, included)
            }
        )
    }
}

@Composable
fun StatusGroupRow(
    group: UserStatusGroup,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("status_item_${group.user.uid}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            photoUrl = group.user.photoUrl,
            displayName = group.user.displayName,
            size = 52.dp,
            showOnlineBadge = false,
            hasStatus = true,
            hasUnviewedStatus = group.hasUnviewed,
            statusCount = group.statuses.size
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.user.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = TimeUtils.formatStatusUploadTime(group.lastUpdated),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
