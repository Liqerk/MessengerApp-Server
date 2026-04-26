package com.messenger.infrastructure.database

import com.messenger.domain.model.MessageType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val login = varchar("login", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val displayName = varchar("display_name", 100).nullable()
    val avatarUrl = varchar("avatar_url", 255).nullable()
    val isOnline = bool("is_online").default(false)
    val lastSeen = timestamp("last_seen").default(Instant.now())
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}

object Messages : Table("messages") {
    val id = integer("id").autoIncrement()
    val clientMessageId = varchar("client_message_id", 100).default("") // ✅ ЗДЕСЬ
    val sender = varchar("sender", 50)
    val receiver = varchar("receiver", 50)
    val text = text("text")
    val timestamp = timestamp("timestamp").default(Instant.now())
    val isRead = bool("is_read").default(false)
    val type = enumerationByName("type", 20, MessageType::class).default(MessageType.TEXT)
    val mediaUrl = varchar("media_url", 500).nullable()
    val duration = integer("duration").default(0)
    val replyToId = integer("reply_to_id").nullable()
    val isFavorite = bool("is_favorite").default(false)
    val isDeleted = bool("is_deleted").default(false) 
    val deletedAt = timestamp("deleted_at").nullable()
    val editedAt = timestamp("edited_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, sender, receiver)
        index(false, timestamp)
        index(false, isRead)
        index(false, clientMessageId) // ✅ индекс
    }
}

object PinnedChats : Table("pinned_chats") {
    val id = integer("id").autoIncrement()
    val userLogin = varchar("user_login", 50)
    val chatWith = varchar("chat_with", 50)
    val pinnedAt = timestamp("pinned_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userLogin, chatWith)
    }
}