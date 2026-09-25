package com.example.model

/**
 * Model representing voice/video call history
 */
data class CallRecord(
    val id: String = "",
    val otherUser: User,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val isMissed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)
