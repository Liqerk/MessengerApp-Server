package com.messenger.infrastructure.database

import com.messenger.domain.model.Message
import com.messenger.domain.model.MessageType
import com.messenger.domain.repository.MessageRepository
import com.messenger.infrastructure.database.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.time.Instant

class ExposedMessageRepository : MessageRepository {
    
    override suspend fun create(message: Message): Message = dbQuery {
    val id = Messages.insert {
        it[clientMessageId] = message.clientMessageId // ✅ НОВОЕ
        it[sender] = message.sender
        it[receiver] = message.receiver
        it[text] = message.text
        it[timestamp] = Instant.now()
        it[type] = message.type
        it[mediaUrl] = message.mediaUrl
        it[duration] = message.duration
        it[replyToId] = message.replyToId
    } get Messages.id

    Messages.select { Messages.id eq id }
        .map { it.toMessage() }
        .single()
}
    
    override suspend fun findById(id: Int): Message? = dbQuery {
        Messages.select { (Messages.id eq id) and (Messages.isDeleted eq false) }
            .map { it.toMessage() }
            .singleOrNull()
    }
    
    override suspend fun getMessages(
        user1: String, 
        user2: String, 
        beforeId: Int?, 
        limit: Int
    ): List<Message> = dbQuery {
        val baseCondition = ((Messages.sender eq user1) and (Messages.receiver eq user2)) or
                           ((Messages.sender eq user2) and (Messages.receiver eq user1))
        
        val condition = if (beforeId != null) {
            baseCondition and (Messages.id less beforeId) and (Messages.isDeleted eq false)
        } else {
            baseCondition and (Messages.isDeleted eq false)
        }
        
        Messages.select { condition }
            .orderBy(Messages.id to SortOrder.DESC)
            .limit(limit)
            .map { it.toMessage() }
            .reversed()
    }
    
    override suspend fun markAsRead(reader: String, sender: String) {
        dbQuery {
            Messages.update({
                (Messages.sender eq sender) and
                (Messages.receiver eq reader) and
                (Messages.isRead eq false)
            }) {
                it[isRead] = true
            }
        }
    }
    
    override suspend fun getUnreadCount(receiver: String, sender: String): Int = dbQuery {
        Messages.select {
            (Messages.sender eq sender) and
            (Messages.receiver eq receiver) and
            (Messages.isRead eq false) and
            (Messages.isDeleted eq false)
        }.count().toInt()
    }
    
    override suspend fun deleteMessage(id: Int, deletedBy: String): Boolean = dbQuery {
    val deleted = Messages.deleteWhere {
        (Messages.id eq id) and (Messages.sender eq deletedBy)
    }
    deleted > 0
}
    override suspend fun deleteChat(user1: String, user2: String): Boolean = dbQuery {
    val deleted = Messages.deleteWhere {
        ((Messages.sender eq user1) and (Messages.receiver eq user2)) or
        ((Messages.sender eq user2) and (Messages.receiver eq user1))
    }
    deleted > 0
}
    
    override suspend fun getLastMessage(user1: String, user2: String): Message? = dbQuery {
        Messages.select {
            (((Messages.sender eq user1) and (Messages.receiver eq user2)) or
            ((Messages.sender eq user2) and (Messages.receiver eq user1))) and
            (Messages.isDeleted eq false)
        }
        .orderBy(Messages.id to SortOrder.DESC)
        .limit(1)
        .map { it.toMessage() }
        .singleOrNull()
    }
    override suspend fun findByClientMessageId(clientMessageId: String): Message? = dbQuery {
    Messages
        .select { (Messages.clientMessageId eq clientMessageId) and (Messages.isDeleted eq false) }
        .map { it.toMessage() }
        .singleOrNull()
}
    override suspend fun toggleFavorite(messageId: Int, userId: String): Boolean = dbQuery {
        val message = Messages.select { 
            (Messages.id eq messageId) and 
            ((Messages.sender eq userId) or (Messages.receiver eq userId))
        }.singleOrNull() ?: return@dbQuery false
        
        val currentFavorite = message[Messages.isFavorite]
        
        Messages.update({ Messages.id eq messageId }) {
            it[isFavorite] = !currentFavorite
        }
        
        !currentFavorite
    }
    
    override suspend fun editMessage(id: Int, newText: String, editedBy: String): Message? = dbQuery {
        val updated = Messages.update({
            (Messages.id eq id) and (Messages.sender eq editedBy) and (Messages.isDeleted eq false)
        }) {
            it[text] = newText
            it[editedAt] = Instant.now()
        }
        
        if (updated > 0) {
            Messages.select { Messages.id eq id }
                .map { it.toMessage() }
                .singleOrNull()
        } else null
    }
    
    private fun ResultRow.toMessage() = Message(
    id = this[Messages.id],
    clientMessageId = this[Messages.clientMessageId], // ✅ НОВОЕ
    sender = this[Messages.sender],
    receiver = this[Messages.receiver],
    text = this[Messages.text],
    timestamp = this[Messages.timestamp].toString(),
    isRead = this[Messages.isRead],
    type = this[Messages.type],
    mediaUrl = this[Messages.mediaUrl],
    duration = this[Messages.duration],
    replyToId = this[Messages.replyToId],
    isFavorite = this[Messages.isFavorite],
    isDeleted = this[Messages.isDeleted],
    editedAt = this[Messages.editedAt]?.toString()
)
}
