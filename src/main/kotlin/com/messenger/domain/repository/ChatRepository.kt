package com.messenger.domain.repository

import com.messenger.domain.model.Chat

interface ChatRepository {
    suspend fun getChatList(userLogin: String): List<Chat>
    suspend fun pinChat(userLogin: String, chatWith: String)
    suspend fun unpinChat(userLogin: String, chatWith: String)
    suspend fun isPinned(userLogin: String, chatWith: String): Boolean
    suspend fun getPinnedChats(userLogin: String): List<String>
}
