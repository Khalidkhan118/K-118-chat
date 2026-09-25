package com.example.model

/**
 * Represents a single chat message with support for delivery & read receipts,
 * deletion for me/everyone, replies, media, and reactions.
 */
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readBy: List<String> = emptyList(),
    val deliveredTo: List<String> = emptyList(),
    val mediaUrl: String = "",
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "VOICE"
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val isDeletedForEveryone: Boolean = false,
    val deletedForUsers: List<String> = emptyList(),
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null
)
