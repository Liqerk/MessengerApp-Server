package com.messenger

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int = 0,
    val login: String,
    val email: String,
    val password: String = "",
    val isOnline: Boolean = false,
    val avatarUrl: String? = null
)

@Serializable
data class Message(
    val id: Int = 0,
    val sender: String,
    val receiver: String,
    val text: String,
    val timestamp: String = "",
    val isRead: Boolean = false,
    val type: String = "text",
    val mediaUrl: String? = null,
    val duration: Int = 0,
    val replyToId: Int = 0,
    val replyToText: String? = null,
    val isFavorite: Boolean = false
)

@Serializable
data class RegisterRequest(
    val login: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

@Serializable
data class ChatItem(
    val id: Int = 0,
    val name: String,
    val image: String = "",
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false
)
