package com.messenger.domain.repository

import com.messenger.domain.model.Message

interface MessageRepository {
    /**
     * Создаёт сообщение и возвращает его полную версию с:
     * - реальным ID из БД
     * - серверным timestamp
     * - дефолтными значениями isRead, type и т.д.
     */
    suspend fun create(message: Message): Message
    suspend fun deleteChat(user1: String, user2: String): Boolean
    suspend fun findById(id: Int): Message?
    suspend fun getMessages(user1: String, user2: String, beforeId: Int?, limit: Int): List<Message>
    suspend fun markAsRead(reader: String, sender: String)
    suspend fun getUnreadCount(receiver: String, sender: String): Int
    suspend fun deleteMessage(id: Int, deletedBy: String): Boolean
    suspend fun getLastMessage(user1: String, user2: String): Message?
    suspend fun toggleFavorite(id: Int, userId: String): Boolean
    suspend fun editMessage(id: Int, newText: String, editedBy: String): Message?
    suspend fun findByClientMessageId(clientMessageId: String): Message?
}