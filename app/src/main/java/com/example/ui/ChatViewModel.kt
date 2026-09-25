package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseChatRepository
import com.example.model.CallRecord
import com.example.model.ChatConversation
import com.example.model.ChatMessage
import com.example.model.StatusItem
import com.example.model.StatusViewer
import com.example.model.User
import com.example.model.UserStatusGroup
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseChatRepository(application)
    private val credentialManager = CredentialManager.create(application)

    val currentUser: StateFlow<User?> = repository.currentUser
    val friends: StateFlow<List<User>> = repository.friends
    val conversations: StateFlow<List<ChatConversation>> = repository.conversations
    val callRecords: StateFlow<List<CallRecord>> = repository.callRecords

    // Search query for friends list / chats
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Firestore database search states
    private val _isSearchingDatabase = MutableStateFlow(false)
    val isSearchingDatabase: StateFlow<Boolean> = _isSearchingDatabase.asStateFlow()

    private val _databaseSearchResults = MutableStateFlow<List<User>>(emptyList())
    val databaseSearchResults: StateFlow<List<User>> = _databaseSearchResults.asStateFlow()

    private var searchJob: Job? = null

    // Filtered friends list
    val filteredFriends: StateFlow<List<User>> = combine(friends, searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.displayName.contains(query, ignoreCase = true) ||
            it.email.contains(query, ignoreCase = true) ||
            it.bio.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active Chat state
    private val _activeChatUser = MutableStateFlow<User?>(null)
    val activeChatUser: StateFlow<User?> = _activeChatUser.asStateFlow()

    private val _activeChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatMessages.asStateFlow()

    // Replying to a specific message
    private val _replyingToMessage = MutableStateFlow<ChatMessage?>(null)
    val replyingToMessage: StateFlow<ChatMessage?> = _replyingToMessage.asStateFlow()

    // Real-time Typing Indicator state
    private val _isOtherUserTyping = MutableStateFlow(false)
    val isOtherUserTyping: StateFlow<Boolean> = _isOtherUserTyping.asStateFlow()

    private val _otherUserTypingName = MutableStateFlow<String?>(null)
    val otherUserTypingName: StateFlow<String?> = _otherUserTypingName.asStateFlow()

    private var typingJob: Job? = null
    private var stopTypingDebounceJob: Job? = null

    // Loading & Status message
    private val _uiStateMessage = MutableStateFlow<String?>(null)
    val uiStateMessage: StateFlow<String?> = _uiStateMessage.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    // Firebase Storage upload states
    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    // Active simulated call state
    private val _activeCall = MutableStateFlow<CallRecord?>(null)
    val activeCall: StateFlow<CallRecord?> = _activeCall.asStateFlow()

    // -------------------------------------------------------------
    // STATUS (STORIES) OBSERVABLE FLOWS
    // -------------------------------------------------------------
    val allStatuses: StateFlow<List<StatusItem>> = repository.statuses

    // Current user's own active statuses
    val myStatuses: StateFlow<List<StatusItem>> = combine(allStatuses, currentUser) { list, me ->
        if (me == null) emptyList()
        else list.filter { it.userId == me.uid }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Friends' statuses grouped by user (filtered for privacy & blocked users)
    val friendsStatusGroups: StateFlow<List<UserStatusGroup>> = combine(
        allStatuses,
        friends,
        currentUser
    ) { statuses, friendList, me ->
        if (me == null) return@combine emptyList<UserStatusGroup>()
        val myUid = me.uid
        val blockedByMe = me.blockedUserIds

        // Group statuses by author
        val otherStatuses = statuses.filter { item ->
            item.userId != myUid && !blockedByMe.contains(item.userId)
        }.filter { item ->
            // Check status privacy rules
            when (item.privacy) {
                "EXCEPT" -> !item.excludedUserIds.contains(myUid)
                "ONLY_SHARE" -> item.includedUserIds.contains(myUid)
                else -> true // MY_CONTACTS
            }
        }

        val grouped = otherStatuses.groupBy { it.userId }
        grouped.mapNotNull { (authorId, items) ->
            val user = friendList.firstOrNull { it.uid == authorId }
                ?: repository.getUser(authorId)
                ?: User(
                    uid = authorId,
                    displayName = items.firstOrNull()?.userName ?: "Friend",
                    photoUrl = items.firstOrNull()?.userPhotoUrl ?: ""
                )

            val hasUnviewed = items.any { item ->
                item.viewers.none { it.userId == myUid }
            }
            val lastUpdated = items.maxOfOrNull { it.timestamp } ?: 0L

            UserStatusGroup(
                user = user,
                statuses = items.sortedBy { it.timestamp },
                hasUnviewed = hasUnviewed,
                lastUpdated = lastUpdated
            )
        }.sortedWith(
            compareByDescending<UserStatusGroup> { it.hasUnviewed }
                .thenByDescending { it.lastUpdated }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        signInAsDemo("khalid_118")
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _databaseSearchResults.value = emptyList()
            _isSearchingDatabase.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearchingDatabase.value = true
            kotlinx.coroutines.delay(200)
            val myUid = currentUser.value?.uid ?: "khalid_118"
            val results = repository.searchUsersInFirestore(query, myUid)
            _databaseSearchResults.value = results
            _isSearchingDatabase.value = false
        }
    }

    fun clearUiMessage() {
        _uiStateMessage.value = null
    }

    fun startGoogleSignIn(activityContext: android.content.Context) {
        viewModelScope.launch {
            _isSigningIn.value = true
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("dummy-client-id.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )

                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val idToken = credential.idToken
                    repository.signInWithGoogle(idToken)
                    _uiStateMessage.value = "Welcome back, ${credential.displayName}!"
                } else {
                    signInAsDemo("khalid_118")
                }
            } catch (e: Exception) {
                Log.w("ChatViewModel", "Credential Manager sign in fallback: ${e.message}")
                signInAsDemo("khalid_118")
                _uiStateMessage.value = "Signed in as Khalid Khan (Demo / Cloud Profile)"
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    fun signInAsDemo(uid: String) {
        viewModelScope.launch {
            _isSigningIn.value = true
            val res = repository.signInAsDemoUser(uid)
            _isSigningIn.value = false
            res.onSuccess {
                _uiStateMessage.value = "Switched to ${it.displayName}"
            }
        }
    }

    fun signInCustom(name: String, email: String) {
        viewModelScope.launch {
            _isSigningIn.value = true
            val res = repository.signInCustom(name, email)
            _isSigningIn.value = false
            res.onSuccess {
                _uiStateMessage.value = "Welcome, ${it.displayName}!"
            }
        }
    }

    fun signOut() {
        stopTypingDebounceJob?.cancel()
        typingJob?.cancel()
        _isOtherUserTyping.value = false
        _otherUserTypingName.value = null
        repository.signOut()
        _activeChatUser.value = null
        _activeChatMessages.value = emptyList()
        _uiStateMessage.value = "Signed out"
    }

    fun openChatWith(user: User) {
        _activeChatUser.value = user
        _isOtherUserTyping.value = false
        _otherUserTypingName.value = null
        _replyingToMessage.value = null
        val myUid = currentUser.value?.uid ?: return
        val chatId = repository.getChatId(myUid, user.uid)

        viewModelScope.launch {
            repository.markMessagesAsRead(chatId, myUid)
            repository.getMessagesFlow(chatId).collect { msgs ->
                // Filter out messages deleted for me
                _activeChatMessages.value = msgs.filter { !it.deletedForUsers.contains(myUid) }
            }
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            repository.getTypingFlow(chatId).collect { typingList ->
                val currentTime = System.currentTimeMillis()
                val otherTyping = typingList.firstOrNull {
                    it.userId != myUid && it.isTyping && (currentTime - it.timestamp < 10000)
                }
                _isOtherUserTyping.value = otherTyping != null
                _otherUserTypingName.value = if (otherTyping != null) {
                    otherTyping.userName.ifBlank { user.displayName }
                } else null
            }
        }
    }

    fun onInputTextChanged(text: String) {
        val myUid = currentUser.value?.uid ?: return
        val myName = currentUser.value?.displayName ?: "User"
        val recipient = _activeChatUser.value ?: return
        val chatId = repository.getChatId(myUid, recipient.uid)

        if (text.isNotBlank()) {
            repository.setTypingStatus(chatId, myUid, myName, true)
            stopTypingDebounceJob?.cancel()
            stopTypingDebounceJob = viewModelScope.launch {
                kotlinx.coroutines.delay(2500)
                repository.setTypingStatus(chatId, myUid, myName, false)
            }
        } else {
            stopTypingDebounceJob?.cancel()
            repository.setTypingStatus(chatId, myUid, myName, false)
        }
    }

    fun closeActiveChat() {
        stopTypingDebounceJob?.cancel()
        typingJob?.cancel()
        val myUid = currentUser.value?.uid
        val recipient = _activeChatUser.value
        if (myUid != null && recipient != null) {
            val chatId = repository.getChatId(myUid, recipient.uid)
            repository.setTypingStatus(chatId, myUid, "", false)
        }
        _activeChatUser.value = null
        _activeChatMessages.value = emptyList()
        _isOtherUserTyping.value = false
        _otherUserTypingName.value = null
        _replyingToMessage.value = null
    }

    fun setReplyingToMessage(message: ChatMessage?) {
        _replyingToMessage.value = message
    }

    fun sendMessage(text: String) {
        stopTypingDebounceJob?.cancel()
        val myUid = currentUser.value?.uid ?: return
        val recipient = _activeChatUser.value ?: return
        if (text.isBlank()) return

        val chatId = repository.getChatId(myUid, recipient.uid)
        repository.setTypingStatus(chatId, myUid, "", false)

        val reply = _replyingToMessage.value

        repository.sendMessage(
            senderId = myUid,
            receiverId = recipient.uid,
            text = text,
            replyToId = reply?.id,
            replyToText = reply?.text?.ifEmpty { if (reply.mediaUrl.isNotBlank()) "Photo" else null },
            replyToSenderName = reply?.let {
                if (it.senderId == myUid) "You" else recipient.displayName
            }
        )

        _replyingToMessage.value = null
    }

    fun sendQuickEmoji(emoji: String) {
        sendMessage(emoji)
    }

    fun sendGalleryImage(imageUri: Uri, caption: String = "", onComplete: () -> Unit = {}) {
        stopTypingDebounceJob?.cancel()
        val myUid = currentUser.value?.uid ?: return
        val recipient = _activeChatUser.value ?: return

        val chatId = repository.getChatId(myUid, recipient.uid)
        repository.setTypingStatus(chatId, myUid, "", false)

        viewModelScope.launch {
            _isUploadingImage.value = true
            _uploadProgress.value = 0
            try {
                repository.sendGalleryImageMessage(
                    senderId = myUid,
                    receiverId = recipient.uid,
                    imageUri = imageUri,
                    caption = caption.trim(),
                    onProgress = { progress ->
                        _uploadProgress.value = progress
                    }
                )
                _uiStateMessage.value = "Photo sent to ${recipient.displayName}"
                onComplete()
            } catch (e: Exception) {
                Log.w("ChatViewModel", "Upload to Firebase Storage failed: ${e.message}")
                repository.sendMessage(
                    senderId = myUid,
                    receiverId = recipient.uid,
                    text = caption.trim(),
                    mediaUrl = imageUri.toString()
                )
                onComplete()
            } finally {
                _isUploadingImage.value = false
                _uploadProgress.value = null
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val myUid = currentUser.value?.uid ?: return
        val recipient = _activeChatUser.value ?: return
        val chatId = repository.getChatId(myUid, recipient.uid)
        repository.toggleReaction(chatId, messageId, myUid, emoji)
    }

    // -------------------------------------------------------------
    // DELETE MESSAGE IMPLEMENTATION
    // -------------------------------------------------------------
    fun deleteMessageForMe(messageId: String) {
        val myUid = currentUser.value?.uid ?: return
        val recipient = _activeChatUser.value ?: return
        val chatId = repository.getChatId(myUid, recipient.uid)
        repository.deleteMessageForMe(chatId, messageId, myUid)
        _uiStateMessage.value = "Message deleted for you"
    }

    fun deleteMessageForEveryone(messageId: String) {
        val myUid = currentUser.value?.uid ?: return
        val recipient = _activeChatUser.value ?: return
        val chatId = repository.getChatId(myUid, recipient.uid)
        repository.deleteMessageForEveryone(chatId, messageId)
        _uiStateMessage.value = "You deleted this message for everyone"
    }

    fun forwardMessage(message: ChatMessage, targetUser: User) {
        val myUid = currentUser.value?.uid ?: return
        repository.sendMessage(
            senderId = myUid,
            receiverId = targetUser.uid,
            text = message.text,
            mediaUrl = message.mediaUrl
        )
        _uiStateMessage.value = "Forwarded message to ${targetUser.displayName}"
    }

    // -------------------------------------------------------------
    // STATUS (STORIES) ACTIONS
    // -------------------------------------------------------------
    fun postTextStatus(text: String, backgroundColorHex: Long) {
        val me = currentUser.value ?: return
        if (text.isBlank()) return

        val newStatus = StatusItem(
            id = "status_" + UUID.randomUUID().toString(),
            userId = me.uid,
            userName = me.displayName,
            userPhotoUrl = me.photoUrl,
            type = "TEXT",
            content = text.trim(),
            backgroundColorHex = backgroundColorHex,
            timestamp = System.currentTimeMillis(),
            viewers = emptyList(),
            privacy = me.statusPrivacy,
            excludedUserIds = me.statusPrivacyExcludedIds,
            includedUserIds = me.statusPrivacyIncludedIds
        )
        repository.postStatus(newStatus)
        _uiStateMessage.value = "Status posted successfully!"
    }

    fun postImageStatus(imageUri: Uri, caption: String) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            _isUploadingImage.value = true
            val uploadRes = repository.uploadChatImageToFirebaseStorage(imageUri, "status_media_${me.uid}")
            val mediaUrl = uploadRes.getOrElse { imageUri.toString() }

            val newStatus = StatusItem(
                id = "status_" + UUID.randomUUID().toString(),
                userId = me.uid,
                userName = me.displayName,
                userPhotoUrl = me.photoUrl,
                type = "IMAGE",
                content = mediaUrl,
                caption = caption.trim(),
                timestamp = System.currentTimeMillis(),
                viewers = emptyList(),
                privacy = me.statusPrivacy,
                excludedUserIds = me.statusPrivacyExcludedIds,
                includedUserIds = me.statusPrivacyIncludedIds
            )
            repository.postStatus(newStatus)
            _isUploadingImage.value = false
            _uiStateMessage.value = "Photo status updated!"
        }
    }

    fun deleteStatus(statusId: String) {
        repository.deleteStatus(statusId)
        _uiStateMessage.value = "Status deleted"
    }

    fun markStatusViewed(statusId: String) {
        val me = currentUser.value ?: return
        val viewer = StatusViewer(
            userId = me.uid,
            userName = me.displayName,
            userPhotoUrl = me.photoUrl,
            viewedAt = System.currentTimeMillis()
        )
        repository.markStatusAsViewed(statusId, viewer)
    }

    fun updateStatusPrivacy(privacy: String, excludedIds: List<String>, includedIds: List<String>) {
        repository.updateStatusPrivacy(privacy, excludedIds, includedIds)
        _uiStateMessage.value = "Status privacy updated"
    }

    fun updateChatWallpaper(wallpaperKey: String) {
        repository.updateChatWallpaper(wallpaperKey)
        _uiStateMessage.value = "Chat background wallpaper updated"
    }

    // -------------------------------------------------------------
    // CALLS
    // -------------------------------------------------------------
    fun startCall(otherUser: User, isVideo: Boolean) {
        val record = CallRecord(
            id = "call_" + UUID.randomUUID().toString().take(8),
            otherUser = otherUser,
            isVideo = isVideo,
            isIncoming = false,
            isMissed = false,
            timestamp = System.currentTimeMillis()
        )
        _activeCall.value = record
        repository.addCallRecord(record)
    }

    fun endCall() {
        _activeCall.value = null
    }

    fun updateUserProfile(displayName: String, bio: String, photoUrl: String) {
        repository.updateUserProfile(displayName, bio, photoUrl)
        _uiStateMessage.value = "Profile updated successfully"
    }

    fun isUserInContacts(userId: String): Boolean {
        val myContacts = currentUser.value?.contactIds ?: emptyList()
        if (myContacts.contains(userId)) return true
        return friends.value.any { it.uid == userId }
    }

    fun addContact(user: User) {
        repository.addContact(user)
        _uiStateMessage.value = "Added ${user.displayName} to your contacts!"
    }

    fun addNewFriend(name: String, email: String, bio: String = "") {
        val uid = "user_" + email.replace(Regex("[^a-zA-Z0-9]"), "").lowercase().take(12)
            .ifEmpty { System.currentTimeMillis().toString().takeLast(6) }

        val avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
        val newUser = User(
            uid = uid,
            displayName = name,
            email = email,
            photoUrl = avatar,
            isOnline = true,
            bio = bio.ifEmpty { "Hey, I'm new to K118!" }
        )
        repository.addContact(newUser)
        _uiStateMessage.value = "Added $name to your contacts!"
    }

    fun setOnlinePresence(isOnline: Boolean) {
        val myUid = currentUser.value?.uid ?: return
        repository.updateOnlineStatus(myUid, isOnline)
    }

    val blockedUsers: StateFlow<List<User>> = combine(currentUser, friends) { current, _ ->
        val blockedIds = current?.blockedUserIds ?: emptyList()
        blockedIds.map { id ->
            repository.getUser(id) ?: User(uid = id, displayName = "User $id")
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun blockUser(targetUserId: String) {
        repository.blockUser(targetUserId)
        val name = repository.getUser(targetUserId)?.displayName ?: "Contact"
        _uiStateMessage.value = "Blocked $name"
    }

    fun unblockUser(targetUserId: String) {
        repository.unblockUser(targetUserId)
        val name = repository.getUser(targetUserId)?.displayName ?: "Contact"
        _uiStateMessage.value = "Unblocked $name"
    }

    fun isUserBlocked(targetUserId: String): Boolean {
        return repository.isUserBlocked(targetUserId)
    }

    fun isCommunicationBlocked(targetUserId: String): Boolean {
        val myUid = currentUser.value?.uid ?: return false
        return repository.isCommunicationBlocked(myUid, targetUserId)
    }
}
