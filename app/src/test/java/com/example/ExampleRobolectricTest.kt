package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FirebaseChatRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("K118", appName)
  }

  @Test
  fun `verify realtime typing status in repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = FirebaseChatRepository(context)

    val chatId = "userA_userB"
    repo.setTypingStatus(chatId, "userB", "User B", true)

    val typingStatuses = repo.getTypingFlow(chatId).value
    val status = typingStatuses.firstOrNull { it.userId == "userB" }
    assertTrue(status != null)
    assertTrue(status!!.isTyping)
    assertEquals("User B", status.userName)

    repo.setTypingStatus(chatId, "userB", "User B", false)
    val updatedStatus = repo.getTypingFlow(chatId).value.firstOrNull { it.userId == "userB" }
    assertTrue(updatedStatus != null)
    assertFalse(updatedStatus!!.isTyping)
  }

  @Test
  fun `verify photo message creation and media url in repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = FirebaseChatRepository(context)

    val senderId = "user_sender"
    val receiverId = "user_receiver"
    val testPhotoUrl = "file:///data/user/0/com.aistudio.k118chat.vxznqw/files/chat_camera_photos/IMG_test.jpg"
    val caption = "Check out this photo!"

    repo.sendMessage(
        senderId = senderId,
        receiverId = receiverId,
        text = caption,
        mediaUrl = testPhotoUrl
    )

    val chatId = repo.getChatId(senderId, receiverId)
    val messages = repo.getMessagesFlow(chatId).value
    val lastMsg = messages.lastOrNull()

    assertTrue(lastMsg != null)
    assertEquals(senderId, lastMsg!!.senderId)
    assertEquals(receiverId, lastMsg.receiverId)
    assertEquals(caption, lastMsg.text)
    assertEquals(testPhotoUrl, lastMsg.mediaUrl)
  }

  @Test
  fun `verify camera photo generation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val (file, uri) = com.example.ui.util.CameraUtils.createSampleCameraPhoto(context)

    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    assertTrue(uri.toString().isNotEmpty())
  }

  @Test
  fun `verify message reaction addition, updating, and removal in repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = FirebaseChatRepository(context)

    val senderId = "user1"
    val receiverId = "user2"
    repo.sendMessage(senderId, receiverId, "Test message for reactions")

    val chatId = repo.getChatId(senderId, receiverId)
    val msgList = repo.getMessagesFlow(chatId).value
    val msg = msgList.last()

    // 1. Add reaction ❤️ from user1
    repo.toggleReaction(chatId, msg.id, senderId, "❤️")
    val updatedList1 = repo.getMessagesFlow(chatId).value
    val updatedMsg1 = updatedList1.first { it.id == msg.id }
    assertEquals("❤️", updatedMsg1.reactions[senderId])

    // 2. Add reaction 🔥 from user2 on the same message
    repo.toggleReaction(chatId, msg.id, receiverId, "🔥")
    val updatedList2 = repo.getMessagesFlow(chatId).value
    val updatedMsg2 = updatedList2.first { it.id == msg.id }
    assertEquals(2, updatedMsg2.reactions.size)
    assertEquals("❤️", updatedMsg2.reactions[senderId])
    assertEquals("🔥", updatedMsg2.reactions[receiverId])

    // 3. User1 switches reaction to 👍
    repo.toggleReaction(chatId, msg.id, senderId, "👍")
    val updatedList3 = repo.getMessagesFlow(chatId).value
    val updatedMsg3 = updatedList3.first { it.id == msg.id }
    assertEquals("👍", updatedMsg3.reactions[senderId])
    assertEquals(2, updatedMsg3.reactions.size)

    // 4. User1 toggles 👍 again -> removes reaction
    repo.toggleReaction(chatId, msg.id, senderId, "👍")
    val updatedList4 = repo.getMessagesFlow(chatId).value
    val updatedMsg4 = updatedList4.first { it.id == msg.id }
    assertFalse(updatedMsg4.reactions.containsKey(senderId))
    assertEquals(1, updatedMsg4.reactions.size)
    assertEquals("🔥", updatedMsg4.reactions[receiverId])
  }

  @Test
  fun `verify gallery image upload and send in repository`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = FirebaseChatRepository(context)

    // Create a temporary sample gallery image file
    val testDir = java.io.File(context.cacheDir, "test_gallery").apply { mkdirs() }
    val testImageFile = java.io.File(testDir, "test_gallery_sample.jpg").apply {
      writeBytes(ByteArray(1024) { 0x2A })
    }
    val testImageUri = android.net.Uri.fromFile(testImageFile)

    val senderId = "sender_gal"
    val receiverId = "receiver_gal"
    val caption = "Sunset from gallery"

    var reportedProgress = -1
    val result = repo.sendGalleryImageMessage(
      senderId = senderId,
      receiverId = receiverId,
      imageUri = testImageUri,
      caption = caption,
      onProgress = { reportedProgress = it }
    )

    assertTrue(result.isSuccess)
    val message = result.getOrNull()
    assertTrue(message != null)
    assertEquals(senderId, message!!.senderId)
    assertEquals(receiverId, message.receiverId)
    assertEquals(caption, message.text)
    assertTrue(message.mediaUrl.isNotEmpty())

    val chatId = repo.getChatId(senderId, receiverId)
    val list = repo.getMessagesFlow(chatId).value
    val sentMsg = list.firstOrNull { it.id == message.id }
    assertTrue(sentMsg != null)
    assertEquals(caption, sentMsg!!.text)
    assertEquals(message.mediaUrl, sentMsg.mediaUrl)
  }

  @Test
  fun `verify Firebase Storage upload helper returns valid media url`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = FirebaseChatRepository(context)

    val tempFile = java.io.File(context.cacheDir, "sample_to_upload.jpg").apply {
      writeBytes(ByteArray(512) { 1 })
    }
    val uri = android.net.Uri.fromFile(tempFile)

    var progressCalled = false
    val uploadResult = repo.uploadChatImageToFirebaseStorage(
      imageUri = uri,
      chatId = "test_chat_storage",
      onProgress = { progressCalled = true }
    )

    assertTrue(uploadResult.isSuccess)
    val mediaUrl = uploadResult.getOrNull()
    assertTrue(mediaUrl != null && mediaUrl.isNotBlank())
  }

  @Test
  fun `verify block and unblock functionality restricts communication`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = FirebaseChatRepository(context)

    // Sign in as user_blocker
    repo.signInAsDemoUser("user_blocker")
    val current = repo.currentUser.value
    assertTrue(current != null)
    assertEquals("user_blocker", current!!.uid)

    val targetUser = "user_blocked"
    assertFalse(repo.isUserBlocked(targetUser))
    assertFalse(repo.isCommunicationBlocked("user_blocker", targetUser))

    // 1. Send normal message before blocking
    repo.sendMessage("user_blocker", targetUser, "Hello before block")
    val chatId = repo.getChatId("user_blocker", targetUser)
    val msgsBefore = repo.getMessagesFlow(chatId).value
    assertEquals(1, msgsBefore.size)
    assertEquals("Hello before block", msgsBefore.first().text)

    // 2. Block user
    repo.blockUser(targetUser)
    assertTrue(repo.isUserBlocked(targetUser))
    assertTrue(repo.isCommunicationBlocked("user_blocker", targetUser))
    assertTrue(repo.currentUser.value!!.blockedUserIds.contains(targetUser))

    // 3. Attempt to send message while blocked -> rejected
    repo.sendMessage("user_blocker", targetUser, "Blocked message should not send")
    val msgsDuring = repo.getMessagesFlow(chatId).value
    assertEquals(1, msgsDuring.size) // still 1

    // Also attempt message in reverse direction -> rejected
    repo.sendMessage(targetUser, "user_blocker", "Blocked reply should not send")
    val msgsDuring2 = repo.getMessagesFlow(chatId).value
    assertEquals(1, msgsDuring2.size)

    // 4. Unblock user -> communication restored
    repo.unblockUser(targetUser)
    assertFalse(repo.isUserBlocked(targetUser))
    assertFalse(repo.isCommunicationBlocked("user_blocker", targetUser))
    assertFalse(repo.currentUser.value!!.blockedUserIds.contains(targetUser))

    // 5. Sending message succeeds after unblocking
    repo.sendMessage("user_blocker", targetUser, "Hello after unblock!")
    val msgsAfter = repo.getMessagesFlow(chatId).value
    assertEquals(2, msgsAfter.size)
    assertEquals("Hello after unblock!", msgsAfter.last().text)
  }
}

