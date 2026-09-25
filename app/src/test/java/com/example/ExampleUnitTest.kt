package com.example

import com.example.model.ChatMessage
import com.example.model.StatusItem
import com.example.model.StatusViewer
import com.example.ui.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testChatMessage_readStatusAndDeletion() {
        val msg = ChatMessage(
            id = "msg1",
            senderId = "u1",
            receiverId = "u2",
            text = "Hello!",
            isRead = false,
            readBy = listOf("u1"),
            deliveredTo = listOf("u1", "u2"),
            isDeletedForEveryone = false,
            deletedForUsers = emptyList()
        )

        assertFalse(msg.isRead)
        assertFalse(msg.readBy.contains("u2"))

        // After reading:
        val readMsg = msg.copy(isRead = true, readBy = msg.readBy + "u2")
        assertTrue(readMsg.isRead)
        assertTrue(readMsg.readBy.contains("u2"))

        // Delete for me
        val deletedForMeMsg = readMsg.copy(deletedForUsers = listOf("u1"))
        assertTrue(deletedForMeMsg.deletedForUsers.contains("u1"))
        assertFalse(deletedForMeMsg.deletedForUsers.contains("u2"))

        // Delete for everyone
        val deletedForEveryoneMsg = readMsg.copy(isDeletedForEveryone = true, text = "")
        assertTrue(deletedForEveryoneMsg.isDeletedForEveryone)
        assertEquals("", deletedForEveryoneMsg.text)
    }

    @Test
    fun testStatusExpiration_24Hours() {
        val now = System.currentTimeMillis()
        val recentStatus = StatusItem(
            id = "s1",
            userId = "u1",
            content = "Recent update",
            timestamp = now - (2 * 60 * 60 * 1000L) // 2 hours ago
        )
        val expiredStatus = StatusItem(
            id = "s2",
            userId = "u1",
            content = "Old update",
            timestamp = now - (25 * 60 * 60 * 1000L) // 25 hours ago
        )

        val cutoff = now - 24 * 60 * 60 * 1000L
        assertTrue(recentStatus.timestamp > cutoff)
        assertFalse(expiredStatus.timestamp > cutoff)
    }

    @Test
    fun testStatusViewer_tracking() {
        val status = StatusItem(
            id = "s1",
            userId = "u1",
            content = "Hello Status",
            viewers = emptyList()
        )

        val viewer = StatusViewer(userId = "u2", userName = "Sarah", viewedAt = System.currentTimeMillis())
        val updatedStatus = status.copy(viewers = status.viewers + viewer)

        assertEquals(1, updatedStatus.viewers.size)
        assertEquals("u2", updatedStatus.viewers.first().userId)
    }

    @Test
    fun testTimeUtils_formatStatusUploadTime() {
        val now = System.currentTimeMillis()
        val justNow = TimeUtils.formatStatusUploadTime(now - 10_000L)
        assertEquals("Just now", justNow)

        val minutesAgo = TimeUtils.formatStatusUploadTime(now - 15 * 60 * 1000L)
        assertEquals("15 minutes ago", minutesAgo)
    }

    @Test
    fun testUserSearch_byDisplayNameAndEmail() {
        val users = listOf(
            com.example.model.User(
                uid = "u1",
                displayName = "Alex Rivera",
                email = "alex.rivera@k118.chat"
            ),
            com.example.model.User(
                uid = "u2",
                displayName = "Sarah Chen",
                email = "sarah.chen@k118.chat"
            ),
            com.example.model.User(
                uid = "u3",
                displayName = "David Kim",
                email = "david.kim@k118.chat"
            )
        )

        // Search by displayName substring
        val nameQuery = "sarah"
        val nameResults = users.filter {
            it.displayName.contains(nameQuery, ignoreCase = true) || it.email.contains(nameQuery, ignoreCase = true)
        }
        assertEquals(1, nameResults.size)
        assertEquals("u2", nameResults.first().uid)

        // Search by email prefix
        val emailQuery = "david.kim"
        val emailResults = users.filter {
            it.displayName.contains(emailQuery, ignoreCase = true) || it.email.contains(emailQuery, ignoreCase = true)
        }
        assertEquals(1, emailResults.size)
        assertEquals("u3", emailResults.first().uid)

        // Search by domain
        val domainQuery = "@k118.chat"
        val domainResults = users.filter {
            it.displayName.contains(domainQuery, ignoreCase = true) || it.email.contains(domainQuery, ignoreCase = true)
        }
        assertEquals(3, domainResults.size)
    }

    @Test
    fun testUserContacts_additionAndState() {
        val currentUser = com.example.model.User(
            uid = "khalid_118",
            displayName = "Khalid Khan",
            contactIds = listOf("alex_rivera")
        )

        assertTrue(currentUser.contactIds.contains("alex_rivera"))
        assertFalse(currentUser.contactIds.contains("david_kim"))

        // Add contact
        val updatedUser = currentUser.copy(
            contactIds = (currentUser.contactIds + "david_kim").distinct()
        )
        assertTrue(updatedUser.contactIds.contains("david_kim"))
        assertEquals(2, updatedUser.contactIds.size)

        // Adding duplicate should not increase size
        val dupUser = updatedUser.copy(
            contactIds = (updatedUser.contactIds + "david_kim").distinct()
        )
        assertEquals(2, dupUser.contactIds.size)
    }

    @Test
    fun testChatId_deterministicBetweenTwoDevices() {
        val device1Uid = "auth_device_1_xyz"
        val device2Uid = "auth_device_2_abc"

        fun getChatId(u1: String, u2: String): String =
            if (u1 < u2) "${u1}_$u2" else "${u2}_$u1"

        val chatIdFromDev1 = getChatId(device1Uid, device2Uid)
        val chatIdFromDev2 = getChatId(device2Uid, device1Uid)

        assertEquals(chatIdFromDev1, chatIdFromDev2)
        assertTrue(chatIdFromDev1.startsWith("auth_device_1_xyz") || chatIdFromDev1.startsWith("auth_device_2_abc"))
    }

    @Test
    fun testTwoDeviceConversationResolution_withUnderscores() {
        val myUid = "user_khalid_118"
        val otherUid = "user_alex_rivera_pro"
        val chatId = if (myUid < otherUid) "${myUid}_$otherUid" else "${otherUid}_$myUid"

        // Resolution without breaking on inner underscores
        val resolvedOtherUid = when {
            chatId.startsWith("${myUid}_") -> chatId.removePrefix("${myUid}_")
            chatId.endsWith("_${myUid}") -> chatId.removeSuffix("_${myUid}")
            else -> ""
        }

        assertEquals(otherUid, resolvedOtherUid)
    }

    @Test
    fun testSearchByFirebaseAuthUid() {
        val users = listOf(
            com.example.model.User(
                uid = "9kL8pQ2mZ",
                displayName = "Device 1 User",
                email = "device1@test.com"
            ),
            com.example.model.User(
                uid = "4vX7wN1rY",
                displayName = "Device 2 User",
                email = "device2@test.com"
            )
        )

        val query = "4vx7"
        val found = users.filter { it.uid.contains(query, ignoreCase = true) }
        assertEquals(1, found.size)
        assertEquals("Device 2 User", found.first().displayName)
    }
}
