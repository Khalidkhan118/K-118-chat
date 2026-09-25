package com.example.model

/**
 * Represents a user in the K118 chat system.
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val bio: String = "Hey there! I am using K118 Chat.",
    val blockedUserIds: List<String> = emptyList(),
    val chatWallpaper: String = "emerald_doodle", // "default", "emerald_doodle", "midnight", "slate", "sunset"
    val statusPrivacy: String = "MY_CONTACTS", // "MY_CONTACTS", "EXCEPT", "ONLY_SHARE"
    val statusPrivacyExcludedIds: List<String> = emptyList(),
    val statusPrivacyIncludedIds: List<String> = emptyList(),
    val contactIds: List<String> = emptyList()
)
