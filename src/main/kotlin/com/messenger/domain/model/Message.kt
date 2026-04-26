package com.messenger.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Message(
    val id: Int = 0,
    val clientMessageId: String = "",
    val sender: String,
    val receiver: String,
    val text: String,
    
    val timestamp: String = Instant.now().toString(),
    val isRead: Boolean = false,
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val duration: Int = 0,
    val replyToId: Int? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val editedAt: String? = null
)

@Serializable
enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, FILE, SYSTEM
}
