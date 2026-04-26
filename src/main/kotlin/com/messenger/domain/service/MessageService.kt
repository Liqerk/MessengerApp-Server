package com.messenger.domain.service

import com.messenger.domain.model.Message
import com.messenger.domain.model.MessageType
import com.messenger.domain.repository.MessageRepository
import com.messenger.domain.repository.UserRepository

class MessageService(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) {
    
    // ✅ ЕДИНСТВЕННАЯ ВЕРСИЯ МЕТОДА
    suspend fun sendMessage(
        sender: String,
        receiver: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null,
        replyToId: Int? = null,
        clientMessageId: String = ""  // ✅ ВОТ ТУТ
    ): Message {
        require(text.isNotBlank() || mediaUrl != null) { "Message cannot be empty" }
        require(sender != receiver) { "Cannot send message to yourself" }

        userRepository.findByLogin(receiver)
            ?: throw IllegalArgumentException("Receiver not found")

        val message = Message(
            clientMessageId = clientMessageId,  // ✅ ПРОБРАСЫВАЕМ
            sender = sender,
            receiver = receiver,
            text = text.trim(),
            type = type,
            mediaUrl = mediaUrl,
            replyToId = replyToId
        )

        return messageRepository.create(message)
    }
    
    suspend fun getMessages(
        user1: String,
        user2: String,
        beforeId: Int? = null,
        limit: Int = 50
    ): List<Message> {
        val safeLimit = limit.coerceIn(1, 100)
        return messageRepository.getMessages(user1, user2, beforeId, safeLimit)
    }
    
    suspend fun markAsRead(reader: String, sender: String) {
        messageRepository.markAsRead(reader, sender)
    }
    
    suspend fun deleteMessage(messageId: Int, deletedBy: String): Boolean {
        return messageRepository.deleteMessage(messageId, deletedBy)
    }
    
    suspend fun editMessage(messageId: Int, newText: String, editedBy: String): Message {
        require(newText.isNotBlank()) { "Message text cannot be empty" }
        
        return messageRepository.editMessage(messageId, newText.trim(), editedBy)
            ?: throw IllegalArgumentException("Message not found or you cannot edit it")
    }
    
    suspend fun toggleFavorite(messageId: Int, userId: String): Boolean {
        return messageRepository.toggleFavorite(messageId, userId)
    }
    
    suspend fun getMessage(id: Int): Message? {
        return messageRepository.findById(id)
    }
    suspend fun deleteChat(user1: String, user2: String): Boolean {
    return messageRepository.deleteChat(user1, user2)
  }
  suspend fun getMessageByClientMessageId(clientMessageId: String): Message? {
    return messageRepository.findByClientMessageId(clientMessageId)
}
}