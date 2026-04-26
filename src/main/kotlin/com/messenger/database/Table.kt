package com.messenger.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    
    val login = varchar("login", 50).uniqueIndex()
    val email = varchar("email", 100)
    val password = varchar("password", 100)
    val isOnline = bool("is_online").default(false)
    val lastSeen = varchar("last_seen", 50).default("")
    val avatarUrl = varchar("avatar_url", 255).nullable()
    
    override val primaryKey = PrimaryKey(id)
}

object Messages : Table("messages") {
    val id = integer("id").autoIncrement()
    val sender = varchar("sender", 50)
    val receiver = varchar("receiver", 50)
    val text = text("text")
    val timestamp = datetime("timestamp").clientDefault { LocalDateTime.now() }
    val isRead = bool("is_read").default(false)
    val type = varchar("type", 20).default("text")
    val mediaUrl = varchar("media_url", 255).nullable()
    val duration = integer("duration").default(0)
    val replyToId = integer("reply_to_id").default(0)
    val replyToText = text("reply_to_text").nullable()
    val isFavorite = bool("is_favorite").default(false)
    
    override val primaryKey = PrimaryKey(id)
}

object PinnedChats : Table("pinned_chats") {
    val id = integer("id").autoIncrement()
    val userLogin = varchar("user_login", 50)
    val chatWith = varchar("chat_with", 50)
    
    override val primaryKey = PrimaryKey(id)
    
    
}