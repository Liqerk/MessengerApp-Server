package com.messenger.presentation.route

import com.messenger.domain.service.WsOnlinePayload 
import com.messenger.config.JwtHelper
import com.messenger.domain.model.MessageType
import com.messenger.domain.service.MessageService
import com.messenger.domain.service.WebSocketService
import com.messenger.domain.service.WsEnvelope
import com.messenger.infrastructure.websocket.ConnectionManager
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LegacyWsMessage(
    val sender: String,
    val receiver: String,
    val text: String,
    val type: String = "text",
    val mediaUrl: String? = null,
    val duration: Int = 0,
    val replyToId: Int = 0,
    val clientMessageId: String = "" // ✅ ДОБАВЬ ЭТО
)
@Serializable
data class WsDeleteChatPayload(
    val chatWith: String
)

fun Route.legacyWebSocketRoute(
    webSocketService: WebSocketService,
    messageService: MessageService
) {
    webSocket("/ws") {
        val token = call.request.queryParameters["token"]
        println("🚨 Token received: ${token?.take(50)}...")
    
        if (token.isNullOrEmpty()) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No token"))
            return@webSocket
        }
        
        val jwtConfig = call.application.attributes.getOrNull(JwtConfigKey)
        if (jwtConfig == null) {
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Config error"))
            return@webSocket
        }
        
        val login = JwtHelper.extractLogin(token, jwtConfig)
        println("🚨 Extracted login: $login")
        if (login == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }
        
        ConnectionManager.addSession(login, this)
        webSocketService.handleConnection(login)
        println("🔌 WebSocket connected: $login")
        
        try {
            incoming.consumeEach { frame ->
                when (frame) {
                    is Frame.Text -> {
                        val messageText = frame.readText()
                        println("📥 WS received from $login: $messageText")
                        
                        // ✅ СНАЧАЛА ПРОБУЕМ ENVELOPE
                        try {
                            val envelope = Json { ignoreUnknownKeys = true }
                                .decodeFromString<WsEnvelope>(messageText)
                            
                            println("✅ Parsed envelope: type=${envelope.type}")
                            webSocketService.handleMessage(login, messageText)
                            
                        } catch (e1: Exception) {
                            // Если не envelope - пробуем Legacy
                            try {
                                val legacyMsg = Json { ignoreUnknownKeys = true }
                                    .decodeFromString<LegacyWsMessage>(messageText)
                                
                                println("✅ Parsed legacy message: ${legacyMsg.sender} → ${legacyMsg.receiver}")
                                
                                // ✅ Сохраняем сообщение с clientMessageId
                                val savedMessage = messageService.sendMessage(
                                    sender = login,
                                    receiver = legacyMsg.receiver,
                                    text = legacyMsg.text,
                                    type = when (legacyMsg.type) {
                                        "image" -> MessageType.IMAGE
                                        "audio" -> MessageType.AUDIO
                                        else -> MessageType.TEXT
                                    },
                                    mediaUrl = legacyMsg.mediaUrl,
                                    replyToId = if (legacyMsg.replyToId > 0) legacyMsg.replyToId else null,
                                    clientMessageId = legacyMsg.clientMessageId // ✅ ПРОБРАСЫВАЕМ
                                )
                                
                                val responseJson = Json.encodeToString(savedMessage)
                                
                                // Отправляем подтверждение отправителю
                                send(Frame.Text(responseJson))
                                println("✅ Sent confirmation to sender: $login")
                                
                                // Отправляем получателю
                                val receiverSessions = ConnectionManager.getSessions(legacyMsg.receiver)
                                
                                if (receiverSessions.isNotEmpty()) {
                                    receiverSessions.forEach { session ->
                                        try {
                                            if (!session.outgoing.isClosedForSend) {
                                                session.send(Frame.Text(responseJson))
                                            }
                                        } catch (e: Exception) {
                                            println("❌ Failed to send to ${legacyMsg.receiver}: ${e.message}")
                                        }
                                    }
                                    println("✅ Forwarded to receiver: ${legacyMsg.receiver}")
                                } else {
                                    println("ℹ️ Receiver ${legacyMsg.receiver} is offline. Online: ${ConnectionManager.getOnlineUsers()}")
                                }
                                
                            } catch (e2: Exception) {
                                println("❌ Failed to parse WS message: ${e2.message}")
                                println("📝 Raw: $messageText")
                            }
                        }
                    }
                    is Frame.Ping -> send(Frame.Pong(frame.data))
                    else -> {}
                }
            }
        } catch (e: Exception) {
            println("❌ WS error for $login: ${e.message}")
        } finally {
    // ✅ СНАЧАЛА ОТПРАВЛЯЕМ ОФФЛАЙН-СТАТУС
    val offlinePayload = Json.encodeToString(WsOnlinePayload(login, false))
    val envelope = WsEnvelope(type = "online", payload = offlinePayload)
    val offlineJson = Json.encodeToString(envelope)
    
    ConnectionManager.getOnlineUsers().forEach { user ->
        if (user != login) {
            ConnectionManager.sendTo(user, offlineJson)
        }
    }
    
    // ✅ ПОТОМ УДАЛЯЕМ СЕССИЮ
    ConnectionManager.removeSession(login, this)
    
    // ✅ И ВЫЗЫВАЕМ handleDisconnection
    webSocketService.handleDisconnection(login)
    
    println("🔌 WebSocket disconnected: $login (offline sent to ${ConnectionManager.getOnlineUsers().size} users)")
}
    }
}