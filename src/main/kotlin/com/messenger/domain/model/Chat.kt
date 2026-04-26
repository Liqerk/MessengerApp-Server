package com.messenger.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Chat(
    val id: String,
    val participant: String,
    val lastMessage: String? = null,
    @kotlinx.serialization.Transient
    val lastMessageTime: Instant? = null,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false
)
