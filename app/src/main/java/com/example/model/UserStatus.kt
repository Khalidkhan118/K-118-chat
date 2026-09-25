package com.example.model

/**
 * Model representing an individual status slide (text or photo)
 */
data class StatusItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val type: String = "TEXT", // "TEXT" or "IMAGE"
    val content: String = "", // Message text for TEXT or image URL for IMAGE
    val caption: String = "",
    val backgroundColorHex: Long = 0xFF0D9488, // Default teal color
    val timestamp: Long = System.currentTimeMillis(),
    val viewers: List<StatusViewer> = emptyList(),
    val privacy: String = "MY_CONTACTS", // "MY_CONTACTS", "EXCEPT", "ONLY_SHARE"
    val excludedUserIds: List<String> = emptyList(),
    val includedUserIds: List<String> = emptyList()
)

/**
 * Record of who viewed the status item and at what time
 */
data class StatusViewer(
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val viewedAt: Long = System.currentTimeMillis()
)

/**
 * Aggregated statuses for a single contact/user
 */
data class UserStatusGroup(
    val user: User,
    val statuses: List<StatusItem>,
    val hasUnviewed: Boolean,
    val lastUpdated: Long
)
