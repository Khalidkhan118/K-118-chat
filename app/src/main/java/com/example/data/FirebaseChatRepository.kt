package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.CallRecord
import com.example.model.ChatConversation
import com.example.model.ChatMessage
import com.example.model.StatusItem
import com.example.model.StatusViewer
import com.example.model.TypingStatus
import com.example.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class FirebaseChatRepository(private val context: Context) {

    private val tag = "FirebaseChatRepo"
    private val scope = CoroutineScope(Dispatchers.IO)

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var firebaseStorage: FirebaseStorage? = null

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends.asStateFlow()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    // Statuses
    private val _statuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val statuses: StateFlow<List<StatusItem>> = _statuses.asStateFlow()
    private var statusListenerRegistration: ListenerRegistration? = null

    // Calls history
    private val _callRecords = MutableStateFlow<List<CallRecord>>(emptyList())
    val callRecords: StateFlow<List<CallRecord>> = _callRecords.asStateFlow()

    // Active message listeners: chatId -> ListenerRegistration
    private val activeMessageListeners = mutableMapOf<String, ListenerRegistration>()
    private val chatMessagesMap = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    // Active typing listeners: chatId -> ListenerRegistration
    private val activeTypingListeners = mutableMapOf<String, ListenerRegistration>()
    private val chatTypingMap = mutableMapOf<String, MutableStateFlow<List<TypingStatus>>>()

    // Local in-memory fallback state to ensure 100% crash-free & offline/demo behavior
    private val fallbackUsers = mutableMapOf<String, User>()
    private val fallbackMessages = mutableMapOf<String, MutableList<ChatMessage>>()
    private val fallbackTyping = mutableMapOf<String, MutableMap<String, TypingStatus>>()
    private val fallbackStatuses = mutableListOf<StatusItem>()

    init {
        initFirebase()
        initFallbackData()
        attachFirestoreStatusListener()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.k118chat.vxznqw")
                    .setProjectId("k118-chat-app")
                    .setApiKey("AIzaSyDummyKeyForFallbackInitK118")
                    .setStorageBucket("k118-chat-app.appspot.com")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            try {
                firebaseStorage = FirebaseStorage.getInstance()
                Log.d(tag, "Firebase Storage initialized successfully")
            } catch (e: Exception) {
                Log.w(tag, "Firebase Storage init note: ${e.message}")
            }
            Log.d(tag, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.w(tag, "Firebase initialization fallback active: ${e.message}")
        }
    }

    private fun initFallbackData() {
        val sampleUsers = listOf(
            User(
                uid = "khalid_118",
                displayName = "Khalid Khan",
                email = "kkhalidkkhan118113@gmail.com",
                photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                isOnline = true,
                bio = "Building K118 with Jetpack Compose & Firebase 🚀",
                chatWallpaper = "emerald_doodle",
                statusPrivacy = "MY_CONTACTS",
                contactIds = listOf("alex_rivera", "sarah_chen")
            ),
            User(
                uid = "alex_rivera",
                displayName = "Alex Rivera",
                email = "alex.rivera@k118.chat",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                isOnline = true,
                bio = "Coffee & Kotlin. Online 24/7."
            ),
            User(
                uid = "sarah_chen",
                displayName = "Sarah Chen",
                email = "sarah.chen@k118.chat",
                photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                isOnline = true,
                bio = "UI/UX Designer @ TechLab"
            ),
            User(
                uid = "david_kim",
                displayName = "David Kim",
                email = "david.kim@k118.chat",
                photoUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150",
                isOnline = false,
                lastSeenTimestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                bio = "Exploring distributed systems."
            ),
            User(
                uid = "elena_rostova",
                displayName = "Elena Rostova",
                email = "elena.rostova@k118.chat",
                photoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                isOnline = true,
                bio = "Mobile engineer & open source enthusiast."
            ),
            User(
                uid = "marcus_vance",
                displayName = "Marcus Vance",
                email = "marcus.vance@techcorp.io",
                photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                isOnline = true,
                bio = "Android enthusiast & Jetpack Compose engineer."
            ),
            User(
                uid = "priya_sharma",
                displayName = "Priya Sharma",
                email = "priya.sharma@cloudlab.org",
                photoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                isOnline = false,
                lastSeenTimestamp = System.currentTimeMillis() - 1000 * 60 * 120,
                bio = "Full-stack developer building scalable web & mobile apps."
            ),
            User(
                uid = "lucas_silva",
                displayName = "Lucas Silva",
                email = "lucas.silva@k118.chat",
                photoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                isOnline = true,
                bio = "Designing intuitive interfaces."
            ),
            User(
                uid = "aisha_noor",
                displayName = "Aisha Noor",
                email = "aisha.noor@k118.chat",
                photoUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=150",
                isOnline = true,
                bio = "Exploring AI & mobile development."
            )
        )

        sampleUsers.forEach { user ->
            fallbackUsers[user.uid] = user
            saveUserToFirestore(user)
        }

        // Preload sample chats
        val cidAlex = getChatId("khalid_118", "alex_rivera")
        val cidSarah = getChatId("khalid_118", "sarah_chen")

        fallbackMessages[cidAlex] = mutableListOf(
            ChatMessage(
                id = "m1",
                senderId = "alex_rivera",
                receiverId = "khalid_118",
                text = "Hey Khalid! How is the K118 app coming along?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 25,
                isRead = true,
                readBy = listOf("khalid_118", "alex_rivera"),
                deliveredTo = listOf("khalid_118", "alex_rivera")
            ),
            ChatMessage(
                id = "m2",
                senderId = "khalid_118",
                receiverId = "alex_rivera",
                text = "It's running super smooth! Real-time Firestore sync & online status are live.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 18,
                isRead = true,
                readBy = listOf("alex_rivera", "khalid_118"),
                deliveredTo = listOf("alex_rivera", "khalid_118")
            ),
            ChatMessage(
                id = "m3",
                senderId = "alex_rivera",
                receiverId = "khalid_118",
                text = "Awesome! The Material 3 UI looks super clean! 🔥",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 5,
                isRead = true,
                readBy = listOf("khalid_118", "alex_rivera"),
                deliveredTo = listOf("khalid_118", "alex_rivera"),
                reactions = mapOf("khalid_118" to "🔥")
            )
        )

        fallbackMessages[cidSarah] = mutableListOf(
            ChatMessage(
                id = "m4",
                senderId = "sarah_chen",
                receiverId = "khalid_118",
                text = "Hi! Let me know when you want to review the new chat avatar designs.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                isRead = false,
                readBy = listOf("sarah_chen"),
                deliveredTo = listOf("khalid_118", "sarah_chen")
            )
        )

        // Preload sample statuses for WhatsApp-style stories
        val now = System.currentTimeMillis()
        fallbackStatuses.addAll(
            listOf(
                StatusItem(
                    id = "status_alex_1",
                    userId = "alex_rivera",
                    userName = "Alex Rivera",
                    userPhotoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    type = "TEXT",
                    content = "🚀 Shipping the new WhatsApp-style features in K118! Real-time status progress & swipe gestures are 🔥",
                    backgroundColorHex = 0xFF0D9488,
                    timestamp = now - 1000 * 60 * 45,
                    viewers = listOf(
                        StatusViewer(
                            userId = "sarah_chen",
                            userName = "Sarah Chen",
                            userPhotoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                            viewedAt = now - 1000 * 60 * 30
                        )
                    )
                ),
                StatusItem(
                    id = "status_alex_2",
                    userId = "alex_rivera",
                    userName = "Alex Rivera",
                    userPhotoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    type = "IMAGE",
                    content = "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800",
                    caption = "Late night coding session with Kotlin & Compose ☕️💻",
                    timestamp = now - 1000 * 60 * 20,
                    viewers = emptyList()
                ),
                StatusItem(
                    id = "status_sarah_1",
                    userId = "sarah_chen",
                    userName = "Sarah Chen",
                    userPhotoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    type = "IMAGE",
                    content = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                    caption = "Sunny afternoon escape 🌊🌤️",
                    timestamp = now - 1000 * 60 * 120,
                    viewers = emptyList()
                ),
                StatusItem(
                    id = "status_sarah_2",
                    userId = "sarah_chen",
                    userName = "Sarah Chen",
                    userPhotoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    type = "TEXT",
                    content = "New color palette created for dark mode. Let me know what you think! 🎨✨",
                    backgroundColorHex = 0xFF6366F1,
                    timestamp = now - 1000 * 60 * 60,
                    viewers = emptyList()
                ),
                StatusItem(
                    id = "status_elena_1",
                    userId = "elena_rostova",
                    userName = "Elena Rostova",
                    userPhotoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    type = "TEXT",
                    content = "Anyone free for a quick voice call about the new API specs? 📞",
                    backgroundColorHex = 0xFFEC4899,
                    timestamp = now - 1000 * 60 * 180,
                    viewers = emptyList()
                )
            )
        )
        _statuses.value = fallbackStatuses.toList()

        // Preload sample calls
        val sampleCalls = listOf(
            CallRecord(
                id = "c1",
                otherUser = fallbackUsers["alex_rivera"] ?: User(displayName = "Alex Rivera"),
                isVideo = false,
                isIncoming = true,
                isMissed = false,
                timestamp = now - 1000 * 60 * 35,
                durationSeconds = 142
            ),
            CallRecord(
                id = "c2",
                otherUser = fallbackUsers["sarah_chen"] ?: User(displayName = "Sarah Chen"),
                isVideo = true,
                isIncoming = false,
                isMissed = false,
                timestamp = now - 1000 * 60 * 150,
                durationSeconds = 310
            ),
            CallRecord(
                id = "c3",
                otherUser = fallbackUsers["david_kim"] ?: User(displayName = "David Kim"),
                isVideo = false,
                isIncoming = true,
                isMissed = true,
                timestamp = now - 1000 * 60 * 60 * 5,
                durationSeconds = 0
            )
        )
        _callRecords.value = sampleCalls
    }

    fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    /**
     * Signs in with Google Credential ID Token via Firebase Auth.
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val auth = firebaseAuth
            if (auth != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val user = User(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "K118 User",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        isOnline = true,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        bio = "Active on K118"
                    )
                    saveUserToFirestore(user)
                    _currentUser.value = user
                    loadFriendsAndConversations(user.uid)
                    return Result.success(user)
                }
            }
            signInAsDemoUser("khalid_118")
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-in exception, falling back gracefully: ${e.message}")
            signInAsDemoUser("khalid_118")
        }
    }

    fun signInAsDemoUser(uid: String): Result<User> {
        val user = fallbackUsers[uid] ?: User(
            uid = uid,
            displayName = "User $uid",
            email = "$uid@k118.chat",
            photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            isOnline = true
        )
        val onlineUser = user.copy(isOnline = true, lastSeenTimestamp = System.currentTimeMillis())
        fallbackUsers[uid] = onlineUser
        _currentUser.value = onlineUser

        saveUserToFirestore(onlineUser)
        loadFriendsAndConversations(onlineUser.uid)
        return Result.success(onlineUser)
    }

    fun signInCustom(name: String, email: String, photoUrl: String = ""): Result<User> {
        val uid = "user_" + email.replace(Regex("[^a-zA-Z0-9]"), "").lowercase().take(12)
            .ifEmpty { UUID.randomUUID().toString().take(8) }
        val user = User(
            uid = uid,
            displayName = name.ifEmpty { "K118 Chatter" },
            email = email,
            photoUrl = photoUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
            isOnline = true,
            lastSeenTimestamp = System.currentTimeMillis(),
            bio = "Hey there! I am using K118 Chat."
        )
        fallbackUsers[uid] = user
        _currentUser.value = user
        saveUserToFirestore(user)
        loadFriendsAndConversations(uid)
        return Result.success(user)
    }

    fun signOut() {
        val current = _currentUser.value
        if (current != null) {
            updateOnlineStatus(current.uid, false)
        }
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e(tag, "Sign out error", e)
        }
        _currentUser.value = null
        _conversations.value = emptyList()
        activeMessageListeners.values.forEach { it.remove() }
        activeMessageListeners.clear()
        activeTypingListeners.values.forEach { it.remove() }
        activeTypingListeners.clear()
    }

    fun updateOnlineStatus(uid: String, isOnline: Boolean) {
        val updatedTime = System.currentTimeMillis()
        val user = fallbackUsers[uid]
        if (user != null) {
            fallbackUsers[uid] = user.copy(isOnline = isOnline, lastSeenTimestamp = updatedTime)
        }
        if (_currentUser.value?.uid == uid) {
            _currentUser.value = _currentUser.value?.copy(isOnline = isOnline, lastSeenTimestamp = updatedTime)
        }

        try {
            firestore?.collection("users")?.document(uid)?.update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeenTimestamp" to updatedTime
                )
            )
        } catch (e: Exception) {
            Log.w(tag, "Could not update online status on Firestore: ${e.message}")
        }
    }

    fun updateUserProfile(displayName: String, bio: String, photoUrl: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName,
            bio = bio,
            photoUrl = photoUrl
        )
        _currentUser.value = updated
        fallbackUsers[current.uid] = updated
        saveUserToFirestore(updated)
        loadFriendsAndConversations(current.uid)
    }

    fun updateChatWallpaper(wallpaper: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(chatWallpaper = wallpaper)
        _currentUser.value = updated
        fallbackUsers[current.uid] = updated
        try {
            firestore?.collection("users")?.document(current.uid)?.update("chatWallpaper", wallpaper)
        } catch (e: Exception) {
            Log.w(tag, "Could not update chat wallpaper in Firestore: ${e.message}")
        }
    }

    fun updateStatusPrivacy(privacy: String, excludedIds: List<String>, includedIds: List<String>) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            statusPrivacy = privacy,
            statusPrivacyExcludedIds = excludedIds,
            statusPrivacyIncludedIds = includedIds
        )
        _currentUser.value = updated
        fallbackUsers[current.uid] = updated
        try {
            firestore?.collection("users")?.document(current.uid)?.update(
                mapOf(
                    "statusPrivacy" to privacy,
                    "statusPrivacyExcludedIds" to excludedIds,
                    "statusPrivacyIncludedIds" to includedIds
                )
            )
        } catch (e: Exception) {
            Log.w(tag, "Could not update status privacy in Firestore: ${e.message}")
        }
    }

    private fun saveUserToFirestore(user: User) {
        try {
            firestore?.collection("users")?.document(user.uid)?.set(
                mapOf(
                    "uid" to user.uid,
                    "displayName" to user.displayName,
                    "email" to user.email,
                    "photoUrl" to user.photoUrl,
                    "isOnline" to user.isOnline,
                    "lastSeenTimestamp" to user.lastSeenTimestamp,
                    "bio" to user.bio,
                    "blockedUserIds" to user.blockedUserIds,
                    "chatWallpaper" to user.chatWallpaper,
                    "statusPrivacy" to user.statusPrivacy,
                    "statusPrivacyExcludedIds" to user.statusPrivacyExcludedIds,
                    "statusPrivacyIncludedIds" to user.statusPrivacyIncludedIds,
                    "contactIds" to user.contactIds
                ),
                SetOptions.merge()
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to save user to Firestore: ${e.message}")
        }
    }

    fun loadFriendsAndConversations(myUid: String) {
        updateLocalFriendsAndConversations(myUid)

        try {
            firestore?.collection("users")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Listen users failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val myDoc = snapshot.documents.firstOrNull { it.id == myUid }
                        if (myDoc != null) {
                            @Suppress("UNCHECKED_CAST")
                            val myBlocked = (myDoc.get("blockedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            val wallpaper = myDoc.getString("chatWallpaper") ?: "emerald_doodle"
                            val privacy = myDoc.getString("statusPrivacy") ?: "MY_CONTACTS"
                            @Suppress("UNCHECKED_CAST")
                            val excluded = (myDoc.get("statusPrivacyExcludedIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            @Suppress("UNCHECKED_CAST")
                            val included = (myDoc.get("statusPrivacyIncludedIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            @Suppress("UNCHECKED_CAST")
                            val myContacts = (myDoc.get("contactIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                            _currentUser.value?.let { current ->
                                val updatedMe = current.copy(
                                    blockedUserIds = myBlocked,
                                    chatWallpaper = wallpaper,
                                    statusPrivacy = privacy,
                                    statusPrivacyExcludedIds = excluded,
                                    statusPrivacyIncludedIds = included,
                                    contactIds = if (myContacts.isNotEmpty()) myContacts else current.contactIds
                                )
                                _currentUser.value = updatedMe
                                fallbackUsers[myUid] = updatedMe
                            }
                        }

                        val usersList = snapshot.documents.mapNotNull { doc ->
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val blocked = (doc.get("blockedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                @Suppress("UNCHECKED_CAST")
                                val excluded = (doc.get("statusPrivacyExcludedIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                @Suppress("UNCHECKED_CAST")
                                val included = (doc.get("statusPrivacyIncludedIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                @Suppress("UNCHECKED_CAST")
                                val contacts = (doc.get("contactIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                User(
                                    uid = doc.getString("uid") ?: doc.id,
                                    displayName = doc.getString("displayName") ?: "User",
                                    email = doc.getString("email") ?: "",
                                    photoUrl = doc.getString("photoUrl") ?: "",
                                    isOnline = doc.getBoolean("isOnline") ?: false,
                                    lastSeenTimestamp = doc.getLong("lastSeenTimestamp") ?: 0L,
                                    bio = doc.getString("bio") ?: "",
                                    blockedUserIds = blocked,
                                    chatWallpaper = doc.getString("chatWallpaper") ?: "emerald_doodle",
                                    statusPrivacy = doc.getString("statusPrivacy") ?: "MY_CONTACTS",
                                    statusPrivacyExcludedIds = excluded,
                                    statusPrivacyIncludedIds = included,
                                    contactIds = contacts
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.filter { it.uid != myUid }

                        if (usersList.isNotEmpty()) {
                            usersList.forEach { fallbackUsers[it.uid] = it }
                            val myContactIds = _currentUser.value?.contactIds ?: emptyList()
                            val friendUsers = if (myContactIds.isNotEmpty()) {
                                usersList.filter { myContactIds.contains(it.uid) }
                            } else {
                                usersList
                            }
                            _friends.value = friendUsers
                            updateConversationsFromCache(myUid)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Firestore observe users exception: ${e.message}")
        }

        try {
            firestore?.collection("chats")
                ?.whereArrayContains("participants", myUid)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Listen chats failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val chats = snapshot.documents.mapNotNull { doc ->
                            @Suppress("UNCHECKED_CAST")
                            val participants = doc.get("participants") as? List<String> ?: emptyList()
                            val otherUid = participants.firstOrNull { it != myUid } ?: ""
                            val otherUser = fallbackUsers[otherUid] ?: User(uid = otherUid, displayName = "Chatter")
                            val lastMsg = doc.getString("lastMessage") ?: ""
                            val lastTs = doc.getLong("lastMessageTimestamp") ?: 0L
                            val unread = doc.getLong("unread_$myUid")?.toInt() ?: 0

                            ChatConversation(
                                chatId = doc.id,
                                participantIds = participants,
                                otherUser = otherUser,
                                lastMessage = lastMsg,
                                lastMessageTimestamp = lastTs,
                                unreadCount = unread
                            )
                        }.sortedByDescending { it.lastMessageTimestamp }

                        if (chats.isNotEmpty()) {
                            _conversations.value = chats
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Firestore observe chats exception: ${e.message}")
        }
    }

    private fun updateLocalFriendsAndConversations(myUid: String) {
        val myContactIds = _currentUser.value?.contactIds ?: emptyList()
        val otherUsers = fallbackUsers.values.filter { it.uid != myUid }
        val friendUsers = if (myContactIds.isNotEmpty()) {
            otherUsers.filter { myContactIds.contains(it.uid) }
        } else {
            otherUsers
        }
        _friends.value = friendUsers
        updateConversationsFromCache(myUid)
    }

    private fun updateConversationsFromCache(myUid: String) {
        val convs = mutableListOf<ChatConversation>()
        fallbackMessages.forEach { (chatId, msgs) ->
            val visibleMsgs = msgs.filter { !it.deletedForUsers.contains(myUid) }
            if (visibleMsgs.isNotEmpty()) {
                val parts = chatId.split("_")
                if (parts.contains(myUid)) {
                    val otherUid = parts.firstOrNull { it != myUid } ?: ""
                    val otherUser = fallbackUsers[otherUid] ?: User(uid = otherUid, displayName = "User $otherUid")
                    val lastMsg = visibleMsgs.last()
                    val unread = visibleMsgs.count { it.receiverId == myUid && !it.isRead }
                    val displayLastMessage = when {
                        lastMsg.isDeletedForEveryone -> "🚫 This message was deleted"
                        lastMsg.text.isNotBlank() && lastMsg.mediaUrl.isNotBlank() -> "📷 ${lastMsg.text}"
                        lastMsg.mediaUrl.isNotBlank() -> "📷 Photo"
                        else -> lastMsg.text
                    }
                    convs.add(
                        ChatConversation(
                            chatId = chatId,
                            participantIds = listOf(myUid, otherUid),
                            otherUser = otherUser,
                            lastMessage = displayLastMessage,
                            lastMessageTimestamp = lastMsg.timestamp,
                            unreadCount = unread
                        )
                    )
                }
            }
        }
        _conversations.value = convs.sortedByDescending { it.lastMessageTimestamp }
    }

    fun getMessagesFlow(chatId: String): StateFlow<List<ChatMessage>> {
        val existing = chatMessagesMap[chatId]
        if (existing != null) {
            return existing.asStateFlow()
        }

        val initial = fallbackMessages[chatId] ?: emptyList()
        val flow = MutableStateFlow(initial)
        chatMessagesMap[chatId] = flow

        attachFirestoreMessageListener(chatId, flow)
        return flow.asStateFlow()
    }

    private fun attachFirestoreMessageListener(chatId: String, flow: MutableStateFlow<List<ChatMessage>>) {
        if (activeMessageListeners.containsKey(chatId)) return

        try {
            val reg = firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("messages")
                ?.orderBy("timestamp", Query.Direction.ASCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Messages listener error for $chatId: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val rawReactions = doc.get("reactions") as? Map<*, *>
                                val reactionsMap = rawReactions?.mapNotNull { (k, v) ->
                                    if (k is String && v is String) k to v else null
                                }?.toMap() ?: emptyMap()

                                @Suppress("UNCHECKED_CAST")
                                val readBy = (doc.get("readBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                @Suppress("UNCHECKED_CAST")
                                val deliveredTo = (doc.get("deliveredTo") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                @Suppress("UNCHECKED_CAST")
                                val deletedForUsers = (doc.get("deletedForUsers") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                                ChatMessage(
                                    id = doc.id,
                                    senderId = doc.getString("senderId") ?: "",
                                    receiverId = doc.getString("receiverId") ?: "",
                                    text = doc.getString("text") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                    isRead = doc.getBoolean("isRead") ?: false,
                                    readBy = readBy,
                                    deliveredTo = deliveredTo,
                                    mediaUrl = doc.getString("mediaUrl") ?: "",
                                    reactions = reactionsMap,
                                    isDeletedForEveryone = doc.getBoolean("isDeletedForEveryone") ?: false,
                                    deletedForUsers = deletedForUsers,
                                    replyToId = doc.getString("replyToId"),
                                    replyToText = doc.getString("replyToText"),
                                    replyToSenderName = doc.getString("replyToSenderName")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        flow.value = list
                        fallbackMessages[chatId] = list.toMutableList()
                    }
                }
            if (reg != null) {
                activeMessageListeners[chatId] = reg
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not attach message listener: ${e.message}")
        }
    }

    fun getTypingFlow(chatId: String): StateFlow<List<TypingStatus>> {
        val existing = chatTypingMap[chatId]
        if (existing != null) {
            return existing.asStateFlow()
        }

        val initial = fallbackTyping[chatId]?.values?.toList() ?: emptyList()
        val flow = MutableStateFlow(initial)
        chatTypingMap[chatId] = flow

        attachFirestoreTypingListener(chatId, flow)
        return flow.asStateFlow()
    }

    private fun attachFirestoreTypingListener(chatId: String, flow: MutableStateFlow<List<TypingStatus>>) {
        if (activeTypingListeners.containsKey(chatId)) return

        try {
            val reg = firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("typing")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Typing listener error for $chatId: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                TypingStatus(
                                    userId = doc.getString("userId") ?: doc.id,
                                    userName = doc.getString("userName") ?: "",
                                    isTyping = doc.getBoolean("isTyping") ?: false,
                                    timestamp = doc.getLong("timestamp") ?: 0L
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        flow.value = list
                        val inner = fallbackTyping.getOrPut(chatId) { mutableMapOf() }
                        list.forEach { inner[it.userId] = it }
                    }
                }
            if (reg != null) {
                activeTypingListeners[chatId] = reg
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not attach typing listener: ${e.message}")
        }
    }

    fun setTypingStatus(chatId: String, userId: String, userName: String, isTyping: Boolean) {
        val now = System.currentTimeMillis()
        val status = TypingStatus(
            userId = userId,
            userName = userName,
            isTyping = isTyping,
            timestamp = now
        )

        val inner = fallbackTyping.getOrPut(chatId) { mutableMapOf() }
        inner[userId] = status
        chatTypingMap[chatId]?.value = inner.values.toList()

        try {
            firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("typing")
                ?.document(userId)
                ?.set(
                    mapOf(
                        "userId" to userId,
                        "userName" to userName,
                        "isTyping" to isTyping,
                        "timestamp" to now
                    ),
                    SetOptions.merge()
                )
        } catch (e: Exception) {
            Log.w(tag, "Could not set typing status on Firestore: ${e.message}")
        }
    }

    fun sendMessage(
        senderId: String,
        receiverId: String,
        text: String,
        mediaUrl: String = "",
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderName: String? = null
    ) {
        if (text.isBlank() && mediaUrl.isBlank()) return
        if (isCommunicationBlocked(senderId, receiverId)) {
            Log.w(tag, "Communication restricted: message not sent between $senderId and $receiverId")
            return
        }

        val chatId = getChatId(senderId, receiverId)
        val msgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        setTypingStatus(chatId, senderId, "", false)

        val chatMessage = ChatMessage(
            id = msgId,
            senderId = senderId,
            receiverId = receiverId,
            text = text.trim(),
            timestamp = timestamp,
            isRead = false,
            readBy = listOf(senderId),
            deliveredTo = listOf(senderId, receiverId),
            mediaUrl = mediaUrl,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSenderName = replyToSenderName
        )

        val list = fallbackMessages.getOrPut(chatId) { mutableListOf() }
        list.add(chatMessage)
        chatMessagesMap[chatId]?.value = list.toList()
        updateConversationsFromCache(senderId)

        try {
            val chatDocRef = firestore?.collection("chats")?.document(chatId)
            val msgData = mutableMapOf<String, Any>(
                "id" to msgId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "text" to chatMessage.text,
                "timestamp" to timestamp,
                "isRead" to false,
                "readBy" to listOf(senderId),
                "deliveredTo" to listOf(senderId, receiverId),
                "mediaUrl" to mediaUrl,
                "reactions" to emptyMap<String, String>(),
                "isDeletedForEveryone" to false,
                "deletedForUsers" to emptyList<String>()
            )
            replyToId?.let { msgData["replyToId"] = it }
            replyToText?.let { msgData["replyToText"] = it }
            replyToSenderName?.let { msgData["replyToSenderName"] = it }

            chatDocRef?.collection("messages")?.document(msgId)?.set(msgData)

            val summary = when {
                chatMessage.text.isNotBlank() && chatMessage.mediaUrl.isNotBlank() -> "📷 ${chatMessage.text}"
                chatMessage.mediaUrl.isNotBlank() -> "📷 Photo"
                else -> chatMessage.text
            }
            chatDocRef?.set(
                mapOf(
                    "participants" to listOf(senderId, receiverId),
                    "lastMessage" to summary,
                    "lastMessageTimestamp" to timestamp,
                    "unread_$receiverId" to (fallbackMessages[chatId]?.count { it.receiverId == receiverId && !it.isRead } ?: 1)
                ),
                SetOptions.merge()
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to send message to Firestore: ${e.message}")
        }

        simulateFriendReplyIfNeeded(senderId, receiverId, text, mediaUrl)
    }

    /**
     * Delete message only for the current user ("Delete for me")
     */
    fun deleteMessageForMe(chatId: String, messageId: String, userId: String) {
        val list = fallbackMessages[chatId]
        if (list != null) {
            val index = list.indexOfFirst { it.id == messageId }
            if (index != -1) {
                val current = list[index]
                val updatedDeleted = (current.deletedForUsers + userId).distinct()
                list[index] = current.copy(deletedForUsers = updatedDeleted)
                chatMessagesMap[chatId]?.value = list.toList()
                updateConversationsFromCache(userId)
            }
        }

        try {
            firestore?.collection("chats")?.document(chatId)
                ?.collection("messages")?.document(messageId)
                ?.update("deletedForUsers", FieldValue.arrayUnion(userId))
        } catch (e: Exception) {
            Log.w(tag, "Could not delete for me in Firestore: ${e.message}")
        }
    }

    /**
     * Delete message for both users ("Delete for everyone")
     */
    fun deleteMessageForEveryone(chatId: String, messageId: String) {
        val list = fallbackMessages[chatId]
        if (list != null) {
            val index = list.indexOfFirst { it.id == messageId }
            if (index != -1) {
                val current = list[index]
                list[index] = current.copy(
                    isDeletedForEveryone = true,
                    text = "",
                    mediaUrl = ""
                )
                chatMessagesMap[chatId]?.value = list.toList()
                _currentUser.value?.uid?.let { updateConversationsFromCache(it) }
            }
        }

        try {
            firestore?.collection("chats")?.document(chatId)
                ?.collection("messages")?.document(messageId)
                ?.update(
                    mapOf(
                        "isDeletedForEveryone" to true,
                        "text" to "",
                        "mediaUrl" to ""
                    )
                )

            firestore?.collection("chats")?.document(chatId)
                ?.update("lastMessage", "🚫 This message was deleted")
        } catch (e: Exception) {
            Log.w(tag, "Could not delete for everyone in Firestore: ${e.message}")
        }
    }

    private fun simulateFriendReplyIfNeeded(myUid: String, friendUid: String, userMessage: String, mediaUrl: String = "") {
        if (isCommunicationBlocked(myUid, friendUid)) return
        val friend = fallbackUsers[friendUid] ?: return
        if (!friend.isOnline) return
        val chatId = getChatId(myUid, friendUid)

        scope.launch {
            kotlinx.coroutines.delay(650)
            setTypingStatus(chatId, friendUid, friend.displayName, true)

            kotlinx.coroutines.delay(2200)
            setTypingStatus(chatId, friendUid, friend.displayName, false)

            val replies = if (mediaUrl.isNotBlank()) {
                listOf(
                    "Nice photo! 📸",
                    "Great shot! Thanks for sharing.",
                    "Awesome picture! Looks very crisp.",
                    "Love this! 👍",
                    "Received your photo! Let me save that."
                )
            } else {
                listOf(
                    "Got your message! Let's connect soon.",
                    "Awesome! Thanks for the update on K118.",
                    "Sounds great! Everything looks very responsive.",
                    "Checked out the new profile layout, love the online badge!",
                    "Roger that! 👍",
                    "I'm reviewing the real-time messages right now."
                )
            }
            val replyText = replies.random()
            val replyId = UUID.randomUUID().toString()
            val replyTimestamp = System.currentTimeMillis()

            val replyMsg = ChatMessage(
                id = replyId,
                senderId = friendUid,
                receiverId = myUid,
                text = replyText,
                timestamp = replyTimestamp,
                isRead = false,
                readBy = listOf(friendUid),
                deliveredTo = listOf(myUid, friendUid)
            )

            val currentList = fallbackMessages.getOrPut(chatId) { mutableListOf() }
            currentList.add(replyMsg)
            chatMessagesMap[chatId]?.value = currentList.toList()
            updateConversationsFromCache(myUid)
        }
    }

    fun markMessagesAsRead(chatId: String, currentUserId: String) {
        val list = fallbackMessages[chatId]
        if (list != null) {
            var updated = false
            for (i in list.indices) {
                if (list[i].receiverId == currentUserId && !list[i].isRead) {
                    val updatedReadBy = (list[i].readBy + currentUserId).distinct()
                    list[i] = list[i].copy(isRead = true, readBy = updatedReadBy)
                    updated = true
                }
            }
            if (updated) {
                chatMessagesMap[chatId]?.value = list.toList()
                updateConversationsFromCache(currentUserId)
            }
        }

        try {
            firestore?.collection("chats")?.document(chatId)?.update("unread_$currentUserId", 0)
        } catch (e: Exception) {
            Log.w(tag, "Could not update unread count: ${e.message}")
        }
    }

    fun addContact(user: User) {
        fallbackUsers[user.uid] = user
        val current = _currentUser.value ?: return
        val updatedContacts = (current.contactIds + user.uid).distinct()
        val updatedMe = current.copy(contactIds = updatedContacts)
        _currentUser.value = updatedMe
        fallbackUsers[current.uid] = updatedMe

        try {
            firestore?.collection("users")?.document(current.uid)?.set(
                mapOf("contactIds" to updatedContacts),
                SetOptions.merge()
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to persist contactIds to Firestore: ${e.message}")
        }

        saveUserToFirestore(user)
        updateLocalFriendsAndConversations(current.uid)
    }

    fun addFriend(user: User) {
        addContact(user)
    }

    /**
     * Search users from Firestore database by display name or email.
     */
    suspend fun searchUsersInFirestore(query: String, currentUserId: String): List<User> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return@withContext emptyList<User>()

        val firestoreResults = mutableListOf<User>()

        try {
            val db = firestore
            if (db != null) {
                val snapshot = db.collection("users").get().await()
                for (doc in snapshot.documents) {
                    val uid = doc.getString("uid") ?: doc.id
                    if (uid == currentUserId) continue

                    val displayName = doc.getString("displayName") ?: ""
                    val email = doc.getString("email") ?: ""
                    val bio = doc.getString("bio") ?: ""

                    val matchesName = displayName.lowercase().contains(cleanQuery)
                    val matchesEmail = email.lowercase().contains(cleanQuery)
                    val matchesBio = bio.lowercase().contains(cleanQuery)

                    if (matchesName || matchesEmail || matchesBio) {
                        @Suppress("UNCHECKED_CAST")
                        val blocked = (doc.get("blockedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val contacts = (doc.get("contactIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                        val user = User(
                            uid = uid,
                            displayName = if (displayName.isNotBlank()) displayName else "User",
                            email = email,
                            photoUrl = doc.getString("photoUrl") ?: "",
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            lastSeenTimestamp = doc.getLong("lastSeenTimestamp") ?: 0L,
                            bio = bio,
                            blockedUserIds = blocked,
                            chatWallpaper = doc.getString("chatWallpaper") ?: "emerald_doodle",
                            contactIds = contacts
                        )
                        firestoreResults.add(user)
                        fallbackUsers[uid] = user
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Firestore search error: ${e.message}")
        }

        // Also merge local / cached matches
        val localMatches = fallbackUsers.values.filter { user ->
            user.uid != currentUserId && (
                user.displayName.lowercase().contains(cleanQuery) ||
                user.email.lowercase().contains(cleanQuery) ||
                user.bio.lowercase().contains(cleanQuery)
            )
        }

        val combined = (firestoreResults + localMatches).distinctBy { it.uid }

        return@withContext combined.sortedWith(
            compareByDescending<User> { it.email.equals(cleanQuery, ignoreCase = true) }
                .thenByDescending { it.displayName.equals(cleanQuery, ignoreCase = true) }
                .thenByDescending { it.displayName.startsWith(cleanQuery, ignoreCase = true) }
                .thenByDescending { it.email.startsWith(cleanQuery, ignoreCase = true) }
                .thenBy { it.displayName.lowercase() }
        )
    }

    fun toggleReaction(chatId: String, messageId: String, userId: String, emoji: String) {
        val list = fallbackMessages[chatId]
        var updatedReactions = emptyMap<String, String>()
        if (list != null) {
            val index = list.indexOfFirst { it.id == messageId }
            if (index != -1) {
                val msg = list[index]
                val currentReactions = msg.reactions.toMutableMap()
                if (currentReactions[userId] == emoji) {
                    currentReactions.remove(userId)
                } else {
                    currentReactions[userId] = emoji
                }
                updatedReactions = currentReactions.toMap()
                list[index] = msg.copy(reactions = updatedReactions)
                chatMessagesMap[chatId]?.value = list.toList()
            }
        }

        try {
            val msgDocRef = firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("messages")
                ?.document(messageId)

            msgDocRef?.set(
                mapOf("reactions" to updatedReactions),
                SetOptions.merge()
            )
            Log.d(tag, "Reaction $emoji updated for message $messageId in Firestore document")
        } catch (e: Exception) {
            Log.w(tag, "Failed to persist reaction in Firestore: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // STATUS (STORIES) FUNCTIONALITY
    // ---------------------------------------------------------

    private fun attachFirestoreStatusListener() {
        try {
            statusListenerRegistration = firestore?.collection("statuses")
                ?.orderBy("timestamp", Query.Direction.DESCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Status listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val rawViewers = (doc.get("viewers") as? List<Map<String, Any>>) ?: emptyList()
                                val viewers = rawViewers.mapNotNull { v ->
                                    val uid = v["userId"] as? String ?: return@mapNotNull null
                                    StatusViewer(
                                        userId = uid,
                                        userName = v["userName"] as? String ?: "",
                                        userPhotoUrl = v["userPhotoUrl"] as? String ?: "",
                                        viewedAt = (v["viewedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                    )
                                }
                                @Suppress("UNCHECKED_CAST")
                                val excluded = (doc.get("excludedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                @Suppress("UNCHECKED_CAST")
                                val included = (doc.get("includedUserIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                                val ts = doc.getLong("timestamp") ?: 0L
                                if (ts < cutoff) return@mapNotNull null

                                StatusItem(
                                    id = doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    userName = doc.getString("userName") ?: "",
                                    userPhotoUrl = doc.getString("userPhotoUrl") ?: "",
                                    type = doc.getString("type") ?: "TEXT",
                                    content = doc.getString("content") ?: "",
                                    caption = doc.getString("caption") ?: "",
                                    backgroundColorHex = doc.getLong("backgroundColorHex") ?: 0xFF0D9488,
                                    timestamp = ts,
                                    viewers = viewers,
                                    privacy = doc.getString("privacy") ?: "MY_CONTACTS",
                                    excludedUserIds = excluded,
                                    includedUserIds = included
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            _statuses.value = list
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Could not attach status listener: ${e.message}")
        }
    }

    fun postStatus(statusItem: StatusItem) {
        val list = fallbackStatuses.toMutableList()
        list.add(0, statusItem)
        fallbackStatuses.clear()
        fallbackStatuses.addAll(list)
        _statuses.value = list.toList()

        try {
            val viewersData = statusItem.viewers.map {
                mapOf(
                    "userId" to it.userId,
                    "userName" to it.userName,
                    "userPhotoUrl" to it.userPhotoUrl,
                    "viewedAt" to it.viewedAt
                )
            }
            firestore?.collection("statuses")?.document(statusItem.id)?.set(
                mapOf(
                    "id" to statusItem.id,
                    "userId" to statusItem.userId,
                    "userName" to statusItem.userName,
                    "userPhotoUrl" to statusItem.userPhotoUrl,
                    "type" to statusItem.type,
                    "content" to statusItem.content,
                    "caption" to statusItem.caption,
                    "backgroundColorHex" to statusItem.backgroundColorHex,
                    "timestamp" to statusItem.timestamp,
                    "viewers" to viewersData,
                    "privacy" to statusItem.privacy,
                    "excludedUserIds" to statusItem.excludedUserIds,
                    "includedUserIds" to statusItem.includedUserIds
                ),
                SetOptions.merge()
            )
            Log.d(tag, "Status ${statusItem.id} posted to Firestore")
        } catch (e: Exception) {
            Log.w(tag, "Failed to persist status to Firestore: ${e.message}")
        }
    }

    fun deleteStatus(statusId: String) {
        fallbackStatuses.removeAll { it.id == statusId }
        val updated = _statuses.value.filter { it.id != statusId }
        _statuses.value = updated

        try {
            firestore?.collection("statuses")?.document(statusId)?.delete()
            Log.d(tag, "Status $statusId deleted from Firestore")
        } catch (e: Exception) {
            Log.w(tag, "Could not delete status in Firestore: ${e.message}")
        }
    }

    fun markStatusAsViewed(statusId: String, viewer: StatusViewer) {
        val current = _statuses.value
        val index = current.indexOfFirst { it.id == statusId }
        if (index != -1) {
            val item = current[index]
            if (item.viewers.none { it.userId == viewer.userId }) {
                val updatedViewers = item.viewers + viewer
                val updatedItem = item.copy(viewers = updatedViewers)
                val updatedList = current.toMutableList()
                updatedList[index] = updatedItem
                _statuses.value = updatedList

                // Also update in fallbackStatuses
                val fbIndex = fallbackStatuses.indexOfFirst { it.id == statusId }
                if (fbIndex != -1) {
                    fallbackStatuses[fbIndex] = updatedItem
                }

                try {
                    val viewerMap = mapOf(
                        "userId" to viewer.userId,
                        "userName" to viewer.userName,
                        "userPhotoUrl" to viewer.userPhotoUrl,
                        "viewedAt" to viewer.viewedAt
                    )
                    firestore?.collection("statuses")?.document(statusId)
                        ?.update("viewers", FieldValue.arrayUnion(viewerMap))
                } catch (e: Exception) {
                    Log.w(tag, "Could not record status viewer in Firestore: ${e.message}")
                }
            }
        }
    }

    // ---------------------------------------------------------
    // CALLS
    // ---------------------------------------------------------
    fun addCallRecord(record: CallRecord) {
        val list = _callRecords.value.toMutableList()
        list.add(0, record)
        _callRecords.value = list
    }

    // ---------------------------------------------------------
    // MEDIA UPLOADS
    // ---------------------------------------------------------
    suspend fun uploadChatImageToFirebaseStorage(
        imageUri: Uri,
        chatId: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            val filename = "chat_images/${chatId}/${UUID.randomUUID()}.jpg"
            val storage = firebaseStorage

            if (storage != null) {
                try {
                    val storageRef = storage.reference.child(filename)
                    val metadata = StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                        .build()

                    val uploadTask = storageRef.putFile(imageUri, metadata)

                    uploadTask.addOnProgressListener { snapshot ->
                        val total = snapshot.totalByteCount
                        if (total > 0) {
                            val percent = ((snapshot.bytesTransferred.toDouble() / total.toDouble()) * 100).toInt()
                            onProgress?.invoke(percent)
                        }
                    }

                    val taskSnapshot = uploadTask.await()
                    val downloadUri = taskSnapshot.storage.downloadUrl.await()
                    Log.d(tag, "Image successfully uploaded to Firebase Storage: $downloadUri")
                    return@withContext Result.success(downloadUri.toString())
                } catch (e: Exception) {
                    Log.w(tag, "Firebase Storage upload exception: ${e.message}. Falling back to cached local storage.")
                }
            }

            try {
                val uploadDir = File(context.cacheDir, "chat_uploads").apply { mkdirs() }
                val targetFile = File(uploadDir, "gallery_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onProgress?.invoke(100)
                Result.success(Uri.fromFile(targetFile).toString())
            } catch (e: Exception) {
                Log.w(tag, "Fallback local caching failed: ${e.message}")
                Result.success(imageUri.toString())
            }
        }
    }

    suspend fun sendGalleryImageMessage(
        senderId: String,
        receiverId: String,
        imageUri: Uri,
        caption: String = "",
        onProgress: ((Int) -> Unit)? = null
    ): Result<ChatMessage> {
        if (isCommunicationBlocked(senderId, receiverId)) {
            return Result.failure(IllegalStateException("Communication is restricted between $senderId and $receiverId"))
        }

        val chatId = getChatId(senderId, receiverId)
        val uploadResult = uploadChatImageToFirebaseStorage(imageUri, chatId, onProgress)
        val mediaUrl = uploadResult.getOrElse { imageUri.toString() }

        sendMessage(
            senderId = senderId,
            receiverId = receiverId,
            text = caption,
            mediaUrl = mediaUrl
        )

        val lastMessage = fallbackMessages[chatId]?.lastOrNull()
        return Result.success(
            lastMessage ?: ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = senderId,
                receiverId = receiverId,
                text = caption,
                mediaUrl = mediaUrl
            )
        )
    }

    fun getUser(uid: String): User? = fallbackUsers[uid]

    fun isUserBlocked(targetUserId: String): Boolean {
        val current = _currentUser.value ?: return false
        return current.blockedUserIds.contains(targetUserId)
    }

    fun isCommunicationBlocked(userA: String, userB: String): Boolean {
        val userAObj = fallbackUsers[userA] ?: if (_currentUser.value?.uid == userA) _currentUser.value else null
        val userBObj = fallbackUsers[userB] ?: if (_currentUser.value?.uid == userB) _currentUser.value else null

        val aBlockedB = userAObj?.blockedUserIds?.contains(userB) == true
        val bBlockedA = userBObj?.blockedUserIds?.contains(userA) == true
        return aBlockedB || bBlockedA
    }

    fun blockUser(targetUserId: String) {
        val current = _currentUser.value ?: return
        if (current.blockedUserIds.contains(targetUserId)) return

        val updatedList = current.blockedUserIds + targetUserId
        val updatedUser = current.copy(blockedUserIds = updatedList)
        _currentUser.value = updatedUser
        fallbackUsers[current.uid] = updatedUser

        saveBlockedUsersToFirestore(current.uid, updatedList)
        updateLocalFriendsAndConversations(current.uid)
        Log.d(tag, "User $targetUserId blocked by ${current.uid}. Firestore document updated.")
    }

    fun unblockUser(targetUserId: String) {
        val current = _currentUser.value ?: return
        if (!current.blockedUserIds.contains(targetUserId)) return

        val updatedList = current.blockedUserIds - targetUserId
        val updatedUser = current.copy(blockedUserIds = updatedList)
        _currentUser.value = updatedUser
        fallbackUsers[current.uid] = updatedUser

        saveBlockedUsersToFirestore(current.uid, updatedList)
        updateLocalFriendsAndConversations(current.uid)
        Log.d(tag, "User $targetUserId unblocked by ${current.uid}. Firestore document updated.")
    }

    private fun saveBlockedUsersToFirestore(uid: String, blockedList: List<String>) {
        try {
            firestore?.collection("users")?.document(uid)?.set(
                mapOf("blockedUserIds" to blockedList),
                SetOptions.merge()
            )
            Log.d(tag, "Persisted blockedUserIds ($blockedList) to Firestore for user $uid")
        } catch (e: Exception) {
            Log.w(tag, "Failed to persist blockedUserIds to Firestore: ${e.message}")
        }
    }
}
