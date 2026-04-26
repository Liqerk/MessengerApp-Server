package com.messenger.domain.service

import kotlinx.serialization.Serializable

@Serializable
data class WsEnvelope(
    val type: String,
    val payload: String
)

@Serializable
data class WsMessagePayload(
    val receiver: String,
    val text: String,
    val replyToId: Int? = null,
    val clientMessageId: String = ""
)

@Serializable
data class WsTypingPayload(
    val sender: String = "",
    val receiver: String,
    val isTyping: Boolean
)

@Serializable
data class WsReadPayload(
    val sender: String,
    val reader: String = ""
)

@Serializable
data class WsDeletePayload(
    val messageId: Int = 0,  // ← для логов
    val clientMessageId: String = ""  // ← основное поле!
)

@Serializable
data class WsOnlinePayload(
    val login: String,
    val isOnline: Boolean
)

@Serializable
data class WsDeleteChatPayload(
    
    val chatWith: String
)