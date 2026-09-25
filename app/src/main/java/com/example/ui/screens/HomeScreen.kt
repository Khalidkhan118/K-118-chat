package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatConversation
import com.example.model.User
import com.example.ui.ChatViewModel
import com.example.ui.components.StatusPrivacyDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.EmeraldOnline
import com.example.ui.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenChat: (User) -> Unit,
    onOpenProfile: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val filteredFriends by viewModel.filteredFriends.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val friendsStatusGroups by viewModel.friendsStatusGroups.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val databaseSearchResults by viewModel.databaseSearchResults.collectAsState()
    val isSearchingDatabase by viewModel.isSearchingDatabase.collectAsState()

    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var chatSubTab by remember { mutableIntStateOf(0) }

    var isSearchActive by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        viewModel.onSearchQueryChange("")
    }

    val user = currentUser
    if (user == null) {
        onNavigateToLogin()
        return
    }

    val totalUnread = conversations.sumOf { it.unreadCount }
    val hasUnviewedStatuses = friendsStatusGroups.any { it.hasUnviewed }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                viewModel.onSearchQueryChange("")
                            },
                            modifier = Modifier.testTag("close_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back from search"
                            )
                        }
                    }
                },
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = {
                                Text(
                                    "Search by name or email...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onSearchQueryChange("") },
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field")
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "K118",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldOnline)
                            )
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = { showAddFriendDialog = true }) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Contact")
                        }
                    } else {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("search_icon_button")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        }

                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Status Privacy") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showPrivacyDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Contact") },
                                leadingIcon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showAddFriendDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings & Wallpaper") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    selectedNavIndex = 3
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedNavIndex == 0,
                    onClick = { selectedNavIndex = 0 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnread > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("$totalUnread")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedNavIndex == 0) Icons.Default.Chat else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Chats"
                            )
                        }
                    },
                    label = { Text("Chats", fontWeight = if (selectedNavIndex == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_chats")
                )

                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = { selectedNavIndex = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (hasUnviewedStatuses) {
                                    Badge(containerColor = EmeraldOnline)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedNavIndex == 1) Icons.Outlined.DonutLarge else Icons.Outlined.Circle,
                                contentDescription = "Status"
                            )
                        }
                    },
                    label = { Text("Status", fontWeight = if (selectedNavIndex == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_status")
                )

                NavigationBarItem(
                    selected = selectedNavIndex == 2,
                    onClick = { selectedNavIndex = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavIndex == 2) Icons.Default.Call else Icons.Outlined.Call,
                            contentDescription = "Calls"
                        )
                    },
                    label = { Text("Calls", fontWeight = if (selectedNavIndex == 2) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_calls")
                )

                NavigationBarItem(
                    selected = selectedNavIndex == 3,
                    onClick = { selectedNavIndex = 3 },
                    icon = {
                        UserAvatar(
                            photoUrl = user.photoUrl,
                            displayName = user.displayName,
                            size = 24.dp,
                            showOnlineBadge = false
                        )
                    },
                    label = { Text("Profile", fontWeight = if (selectedNavIndex == 3) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        },
        floatingActionButton = {
            if (selectedNavIndex == 0 && !isSearchActive) {
                FloatingActionButton(
                    onClick = {
                        if (chatSubTab == 0) {
                            chatSubTab = 1
                        } else {
                            showAddFriendDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("home_fab")
                ) {
                    if (chatSubTab == 0) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = "New Chat")
                    } else {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Contact")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearchActive) {
                SearchResultsContent(
                    searchQuery = searchQuery,
                    isSearching = isSearchingDatabase,
                    searchResults = databaseSearchResults,
                    existingFriends = friends,
                    onOpenChat = { targetUser ->
                        isSearchActive = false
                        viewModel.onSearchQueryChange("")
                        onOpenChat(targetUser)
                    },
                    onAddContact = { targetUser ->
                        viewModel.addContact(targetUser)
                    },
                    onSelectQuickQuery = { query ->
                        viewModel.onSearchQueryChange(query)
                    },
                    onAddNewCustomUser = {
                        showAddFriendDialog = true
                    },
                    isUserInContacts = { uid -> viewModel.isUserInContacts(uid) }
                )
            } else {
                when (selectedNavIndex) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = chatSubTab,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = chatSubTab == 0,
                                    onClick = { chatSubTab = 0 },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Chats (${conversations.size})", fontWeight = if (chatSubTab == 0) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    modifier = Modifier.testTag("subtab_chats")
                                )

                                Tab(
                                    selected = chatSubTab == 1,
                                    onClick = { chatSubTab = 1 },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Contacts (${filteredFriends.size})", fontWeight = if (chatSubTab == 1) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    modifier = Modifier.testTag("subtab_contacts")
                                )
                            }

                            if (chatSubTab == 0) {
                                ChatsListContent(
                                    conversations = conversations,
                                    blockedUserIds = user.blockedUserIds,
                                    onOpenChat = onOpenChat,
                                    onGoToFriends = { chatSubTab = 1 }
                                )
                            } else {
                                FriendsListContent(
                                    friends = filteredFriends,
                                    blockedUserIds = user.blockedUserIds,
                                    onStartChat = onOpenChat,
                                    onAddFriendClick = { showAddFriendDialog = true }
                                )
                            }
                        }
                    }

                    1 -> {
                        StatusTabContent(viewModel = viewModel)
                    }

                    2 -> {
                        CallsTabContent(viewModel = viewModel)
                    }

                    3 -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onBack = { selectedNavIndex = 0 },
                            onLoggedOut = onNavigateToLogin
                        )
                    }
                }
            }
        }
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            initialQuery = searchQuery,
            onDismiss = { showAddFriendDialog = false },
            onAddFriend = { name, email, bio ->
                viewModel.addNewFriend(name, email, bio)
                showAddFriendDialog = false
            }
        )
    }

    if (showPrivacyDialog) {
        StatusPrivacyDialog(
            currentPrivacy = user.statusPrivacy,
            excludedIds = user.statusPrivacyExcludedIds,
            includedIds = user.statusPrivacyIncludedIds,
            friends = friends,
            onDismiss = { showPrivacyDialog = false },
            onSavePrivacy = { privacy, excluded, included ->
                viewModel.updateStatusPrivacy(privacy, excluded, included)
            }
        )
    }
}

@Composable
fun ChatsListContent(
    conversations: List<ChatConversation>,
    blockedUserIds: List<String> = emptyList(),
    onOpenChat: (User) -> Unit,
    onGoToFriends: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (conversations.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Conversations Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Start a 1-to-1 real-time conversation with your friends on K118.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onGoToFriends,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("browse_friends_button")
                ) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Contacts")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(conversations, key = { it.chatId }) { conv ->
                val other = conv.otherUser ?: User(displayName = "Chatter")
                val isBlocked = blockedUserIds.contains(other.uid)

                ConversationItem(
                    conversation = conv,
                    otherUser = other,
                    isBlocked = isBlocked,
                    onClick = { onOpenChat(other) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ChatConversation,
    otherUser: User,
    isBlocked: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_item_${otherUser.uid}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            photoUrl = otherUser.photoUrl,
            displayName = otherUser.displayName,
            size = 52.dp,
            isOnline = otherUser.isOnline && !isBlocked,
            showOnlineBadge = !isBlocked
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = otherUser.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isBlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Blocked",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = TimeUtils.formatConversationTime(conversation.lastMessageTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBlocked) "🚫 Messages restricted" else conversation.lastMessage.ifEmpty { "Tap to send a message" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (conversation.unreadCount > 0 && !isBlocked) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (conversation.unreadCount > 0 && !isBlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0 && !isBlocked) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EmeraldOnline)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendsListContent(
    friends: List<User>,
    blockedUserIds: List<String> = emptyList(),
    onStartChat: (User) -> Unit,
    onAddFriendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (friends.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No contacts found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddFriendClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_first_friend_button")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Contact")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(friends, key = { it.uid }) { friend ->
                val isBlocked = blockedUserIds.contains(friend.uid)
                FriendItem(
                    user = friend,
                    isBlocked = isBlocked,
                    onChatClick = { onStartChat(friend) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun FriendItem(
    user: User,
    isBlocked: Boolean = false,
    onChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("friend_item_${user.uid}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            photoUrl = user.photoUrl,
            displayName = user.displayName,
            size = 50.dp,
            isOnline = user.isOnline && !isBlocked,
            showOnlineBadge = !isBlocked
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isBlocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Blocked",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (user.isOnline) "• Online" else "• Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (user.isOnline) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (isBlocked) "Communication restricted" else user.bio.ifEmpty { user.email },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = TimeUtils.formatLastSeen(user.isOnline && !isBlocked, user.lastSeenTimestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        IconButton(
            onClick = onChatClick,
            modifier = Modifier.testTag("message_button_${user.uid}")
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Message ${user.displayName}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AddFriendDialog(
    initialQuery: String = "",
    onDismiss: () -> Unit,
    onAddFriend: (name: String, email: String, bio: String) -> Unit
) {
    var name by remember {
        mutableStateOf(if (initialQuery.contains("@")) "" else initialQuery)
    }
    var email by remember {
        mutableStateOf(if (initialQuery.contains("@")) initialQuery else "")
    }
    var bio by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact to K118", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Add contacts to chat with them in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_friend_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Contact's Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_friend_email_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Status / Bio (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onAddFriend(name, email, bio)
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.testTag("dialog_confirm_add_friend")
            ) {
                Text("Add Contact")
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
fun SearchResultsContent(
    searchQuery: String,
    isSearching: Boolean,
    searchResults: List<User>,
    existingFriends: List<User>,
    onOpenChat: (User) -> Unit,
    onAddContact: (User) -> Unit,
    onSelectQuickQuery: (String) -> Unit,
    onAddNewCustomUser: () -> Unit,
    isUserInContacts: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (isSearching) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_progress_indicator"),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (searchQuery.isBlank()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonSearch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Search Firestore Users",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Find anyone by display name or email to add to contacts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Suggested Searches",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    val quickQueries = listOf("David Kim", "Elena", "Marcus", "Priya", "Alex", "Sarah", "@k118.chat")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(quickQueries) { suggestion ->
                            AssistChip(
                                onClick = { onSelectQuickQuery(suggestion) },
                                label = { Text(suggestion) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                if (existingFriends.isNotEmpty()) {
                    item {
                        Text(
                            text = "Existing Contacts (${existingFriends.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(existingFriends, key = { it.uid }) { friend ->
                        SearchResultUserItem(
                            user = friend,
                            isInContacts = true,
                            onOpenChat = { onOpenChat(friend) },
                            onAddContact = {}
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        } else {
            if (searchResults.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No users found in Firestore",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No registered user matches \"$searchQuery\" by name or email.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddNewCustomUser,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_custom_contact_button")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add as New Contact")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Firestore Database Results (${searchResults.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isSearching) {
                                Text(
                                    text = "Searching...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(searchResults, key = { it.uid }) { user ->
                        val inContacts = isUserInContacts(user.uid)
                        SearchResultUserItem(
                            user = user,
                            isInContacts = inContacts,
                            onOpenChat = { onOpenChat(user) },
                            onAddContact = { onAddContact(user) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultUserItem(
    user: User,
    isInContacts: Boolean,
    onOpenChat: () -> Unit,
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenChat() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("search_result_item_${user.uid}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            photoUrl = user.photoUrl,
            displayName = user.displayName,
            size = 48.dp,
            isOnline = user.isOnline,
            showOnlineBadge = true
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.isOnline) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldOnline)
                    )
                }
            }

            if (user.email.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (user.bio.isNotBlank()) {
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "UID: ${user.uid}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isInContacts) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Contact",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            IconButton(
                onClick = onOpenChat,
                modifier = Modifier.testTag("chat_button_${user.uid}")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Button(
                onClick = onAddContact,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_contact_${user.uid}")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
