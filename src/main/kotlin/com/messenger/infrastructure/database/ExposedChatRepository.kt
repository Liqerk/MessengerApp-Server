package com.messenger.infrastructure.database

import com.messenger.domain.model.Chat
import com.messenger.domain.repository.ChatRepository
import com.messenger.infrastructure.database.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ExposedChatRepository : ChatRepository {
    
    override suspend fun getChatList(userLogin: String): List<Chat> = dbQuery {
        val partners = Messages.select {
            ((Messages.sender eq userLogin) or (Messages.receiver eq userLogin)) and
            (Messages.isDeleted eq false)
        }.map { row ->
            if (row[Messages.sender] == userLogin) row[Messages.receiver] 
            else row[Messages.sender]
        }.distinct()
        
        val pinnedChats = PinnedChats.select { PinnedChats.userLogin eq userLogin }
            .map { it[PinnedChats.chatWith] }
            .toSet()
        
        partners.map { partner ->
            val lastMsg = Messages.select {
                (((Messages.sender eq userLogin) and (Messages.receiver eq partner)) or
                ((Messages.sender eq partner) and (Messages.receiver eq userLogin))) and
                (Messages.isDeleted eq false)
            }
            .orderBy(Messages.id to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            
            val unreadCount = Messages.select {
                (Messages.sender eq partner) and
                (Messages.receiver eq userLogin) and
                (Messages.isRead eq false) and
                (Messages.isDeleted eq false)
            }.count().toInt()
            
            val isOnline = Users.select { Users.login eq partner }
                .singleOrNull()
                ?.get(Users.isOnline) ?: false
            
            Chat(
                id = "${minOf(userLogin, partner)}_${maxOf(userLogin, partner)}",
                participant = partner,
                lastMessage = lastMsg?.get(Messages.text),
                lastMessageTime = lastMsg?.get(Messages.timestamp),
                unreadCount = unreadCount,
                isOnline = isOnline,
                isPinned = partner in pinnedChats
            )
        }.sortedWith(
            compareByDescending<Chat> { it.isPinned }
                .thenByDescending { it.lastMessageTime }
        )
    }
    
    override suspend fun pinChat(userLogin: String, chatWith: String) {
        dbQuery {
            PinnedChats.insertIgnore {
                it[PinnedChats.userLogin] = userLogin
                it[PinnedChats.chatWith] = chatWith
            }
        }
    }
    
    override suspend fun unpinChat(userLogin: String, chatWith: String) {
        dbQuery {
            PinnedChats.deleteWhere {
                (PinnedChats.userLogin eq userLogin) and (PinnedChats.chatWith eq chatWith)
            }
        }
    }
    
    override suspend fun isPinned(userLogin: String, chatWith: String): Boolean = dbQuery {
        PinnedChats.select {
            (PinnedChats.userLogin eq userLogin) and (PinnedChats.chatWith eq chatWith)
        }.count() > 0
    }
    
    override suspend fun getPinnedChats(userLogin: String): List<String> = dbQuery {
        PinnedChats.select { PinnedChats.userLogin eq userLogin }
            .map { it[PinnedChats.chatWith] }
    }
}
