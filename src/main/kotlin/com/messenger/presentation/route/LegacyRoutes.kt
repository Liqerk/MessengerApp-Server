package com.messenger.presentation.route

import com.messenger.domain.service.*
import com.messenger.infrastructure.websocket.ConnectionManager
import com.messenger.presentation.dto.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class LegacyAuthResponse(
    val token: String,
    val user: LegacyUserDto
)

@Serializable
data class LegacyUserDto(
    val id: Int = 0,
    val login: String,
    val mail: String,
    val isOnline: Boolean = false,
    val avatarUrl: String? = null
)

@Serializable
data class LegacyMessageDto(
    val id: Int = 0,
    val clientMessageId: String = "",
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
data class LegacyChatItemDto(
    val id: Int = 0,
    val name: String,
    val image: String = "",
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false
)

@Serializable
data class LegacySendRequest(
    val sender: String,
    val receiver: String,
    val text: String,
    val clientMessageId: String = ""
)

fun Route.legacyRoutes(
    authService: AuthService,
    userService: UserService,
    messageService: MessageService,
    chatService: ChatService
) {
    
    post("/auth/register") {
        try {
            val req = call.receive<RegisterRequest>()
            val response = authService.register(req)
            
            val legacy = LegacyAuthResponse(
                token = response.accessToken,
                user = LegacyUserDto(
                    id = response.user.id,
                    login = response.user.login,
                    mail = response.user.email,
                    isOnline = response.user.isOnline,
                    avatarUrl = response.user.avatarUrl
                )
            )
            
            call.respond(HttpStatusCode.Created, legacy)
            println("✅ Registered: ${req.login}")
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.Conflict, LegacyError(e.message ?: "Error"))
        } catch (e: Exception) {
            println("❌ Register error: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, LegacyError("Registration failed"))
        }
    }
    
    post("/auth/login") {
        try {
            val req = call.receive<LoginRequest>()
            val response = authService.login(req)
            
            val legacy = LegacyAuthResponse(
                token = response.accessToken,
                user = LegacyUserDto(
                    id = response.user.id,
                    login = response.user.login,
                    mail = response.user.email,
                    isOnline = response.user.isOnline,
                    avatarUrl = response.user.avatarUrl
                )
            )
            
            call.respond(HttpStatusCode.OK, legacy)
            println("✅ Login: ${req.login}")
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.Unauthorized, LegacyError("Invalid credentials"))
        } catch (e: Exception) {
            println("❌ Login error: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, LegacyError("Login failed"))
        }
    }
    
    get("/messages") {
        try {
            val user1 = call.request.queryParameters["user1"] ?: ""
            val user2 = call.request.queryParameters["user2"] ?: ""
            
            if (user1.isEmpty() || user2.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, LegacyError("user1 and user2 required"))
                return@get
            }
            
            val messages = messageService.getMessages(user1, user2)
            
            val legacy = messages.map { msg ->
                LegacyMessageDto(
                    id = msg.id,
                    clientMessageId = msg.clientMessageId,
                    sender = msg.sender,
                    receiver = msg.receiver,
                    text = msg.text,
                    timestamp = msg.timestamp,
                    isRead = msg.isRead,
                    type = msg.type.name.lowercase(),
                    mediaUrl = msg.mediaUrl,
                    duration = msg.duration,
                    replyToId = msg.replyToId ?: 0,
                    isFavorite = msg.isFavorite
                )
            }
            
            call.respond(HttpStatusCode.OK, legacy)
        } catch (e: Exception) {
            println("❌ Get messages error: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, LegacyError("Failed"))
        }
    }
    
    post("/messages") {
      
        try {
            val req = call.receive<LegacySendRequest>()
            
            val savedMessage = messageService.sendMessage(
                sender = req.sender,
                receiver = req.receiver,
                text = req.text,
                clientMessageId = req.clientMessageId
            )
            
            val response = LegacyMessageDto(
                id = savedMessage.id,
                clientMessageId = savedMessage.clientMessageId,
                sender = savedMessage.sender,
                receiver = savedMessage.receiver,
                text = savedMessage.text,
                timestamp = savedMessage.timestamp,
                isRead = savedMessage.isRead,
                type = savedMessage.type.name.lowercase(),
                mediaUrl = savedMessage.mediaUrl,
                duration = savedMessage.duration,
                replyToId = savedMessage.replyToId ?: 0,
                isFavorite = savedMessage.isFavorite
            )
            
            call.respond(HttpStatusCode.Created, response)
            println("📤 HTTP Message: ${req.sender} → ${req.receiver} (CID=${req.clientMessageId})")
        } catch (e: Exception) {
            println("❌ Send error: ${e.message}")
            call.respond(HttpStatusCode.BadRequest, LegacyError(e.message ?: "Failed"))
        }
    }
    delete("/messages/{id}") {
    try {
        val messageId = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                LegacyError("Message ID required")
            )

        val deleted = messageService.deleteMessage(messageId, "") // TODO: добавить auth

        if (deleted) {
            call.respond(HttpStatusCode.OK, LegacySuccess(true))
        } else {
            call.respond(HttpStatusCode.NotFound, LegacyError("Not found"))
        }
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, LegacyError("Failed"))
    }
}
    get("/chats") {
        try {
            val user = call.request.queryParameters["user"] ?: ""
            
            if (user.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, LegacyError("user required"))
                return@get
            }
            
            val chats = chatService.getChatList(user)
            
            val legacy = chats.map { chat ->
                LegacyChatItemDto(
                    name = chat.participant,
                    lastMessage = chat.lastMessage ?: "",
                    lastMessageTime = chat.lastMessageTime?.toString() ?: "",
                    unreadCount = chat.unreadCount,
                    isOnline = ConnectionManager.isOnline(chat.participant),  // ✅ ИСПРАВЛЕНО
                    isPinned = chat.isPinned
                )
            }
            
            call.respond(HttpStatusCode.OK, legacy)
        } catch (e: Exception) {
            println("❌ Get chats error: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, LegacyError("Failed"))
        }
    }
    
    get("/users/search") {
        try {
            val query = call.request.queryParameters["q"] ?: ""
            val except = call.request.queryParameters["except"] ?: ""
            
            if (query.isEmpty()) {
                call.respond(HttpStatusCode.OK, emptyList<LegacyUserDto>())
                return@get
            }
            
            val users = userService.search(query, except)
            
            val legacy = users.map { user ->
                LegacyUserDto(
                    id = user.id,
                    login = user.login,
                    mail = user.email,
                    isOnline = ConnectionManager.isOnline(user.login),  // ✅ ТУТ ПРАВИЛЬНО
                    avatarUrl = user.avatarUrl
                )
            }
            
            call.respond(HttpStatusCode.OK, legacy)
        } catch (e: Exception) {
            println("❌ Search error: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, LegacyError("Failed"))
        }
    }
}

@Serializable
data class LegacyError(val error: String)

@Serializable
data class LegacySuccess(val success: Boolean)