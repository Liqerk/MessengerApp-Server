package com.messenger.domain.service

import com.messenger.domain.model.Chat
import com.messenger.domain.repository.ChatRepository
import com.messenger.domain.repository.MessageRepository

class ChatService(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository
) {
    
    suspend fun getChatList(userLogin: String): List<Chat> {
        return chatRepository.getChatList(userLogin)
    }
    
    suspend fun pinChat(userLogin: String, chatWith: String) {
        chatRepository.pinChat(userLogin, chatWith)
    }
    
    suspend fun unpinChat(userLogin: String, chatWith: String) {
        chatRepository.unpinChat(userLogin, chatWith)
    }
    
    suspend fun togglePinChat(userLogin: String, chatWith: String): Boolean {
        val isPinned = chatRepository.isPinned(userLogin, chatWith)
        if (isPinned) {
            chatRepository.unpinChat(userLogin, chatWith)
        } else {
            chatRepository.pinChat(userLogin, chatWith)
        }
        return !isPinned
    }
}
