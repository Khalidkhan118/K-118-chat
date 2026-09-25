package com.example.model

/**
 * Summary of a 1-to-1 conversation shown on the main chat list.
 */
data class ChatConversation(
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val otherUser: User? = null,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0
)
