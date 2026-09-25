package com.example.model

data class TypingStatus(
    val userId: String = "",
    val userName: String = "",
    val isTyping: Boolean = false,
    val timestamp: Long = 0L
)
