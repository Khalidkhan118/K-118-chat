package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    fun formatMessageTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatConversationTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isToday = msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            return formatMessageTime(timestamp)
        }

        val isYesterday = msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1

        if (isYesterday) {
            return "Yesterday"
        }

        if (diff < 7 * 24 * 60 * 60 * 1000L) {
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            return dayFormat.format(Date(timestamp))
        }

        val fullFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        return fullFormat.format(Date(timestamp))
    }

    fun formatStatusUploadTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minutes = diff / (60 * 1000)

        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isToday = msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            isToday -> {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Today, ${sdf.format(Date(timestamp))}"
            }
            else -> {
                val sdf = SimpleDateFormat("Yesterday, h:mm a", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    fun formatLastSeen(isOnline: Boolean, lastSeenTimestamp: Long): String {
        if (isOnline) return "Online"
        if (lastSeenTimestamp <= 0L) return "Offline"

        val diff = System.currentTimeMillis() - lastSeenTimestamp
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)

        return when {
            minutes < 1 -> "Last seen just now"
            minutes < 60 -> "Last seen $minutes min ago"
            hours < 24 -> "Last seen $hours h ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                "Last seen ${sdf.format(Date(lastSeenTimestamp))}"
            }
        }
    }

    fun formatDateHeader(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isToday = msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        if (isToday) return "Today"

        val isYesterday = msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1
        if (isYesterday) return "Yesterday"

        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
