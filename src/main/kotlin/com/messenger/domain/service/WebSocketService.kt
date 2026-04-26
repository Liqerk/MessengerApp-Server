package com.messenger.domain.service

import com.messenger.domain.model.Message
import com.messenger.infrastructure.websocket.ConnectionManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WebSocketService(
    private val connectionManager: ConnectionManager,
    private val messageService: MessageService,
    private val userService: UserService
) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun handleConnection(login: String) {
        userService.setOnlineStatus(login, true)
        broadcastOnlineStatus(login, true)
    }
    
    suspend fun handleDisconnection(login: String) {
        if (!connectionManager.isOnline(login)) {
            userService.setOnlineStatus(login, false)
            broadcastOnlineStatus(login, false)
        }
    }
    
    suspend fun handleMessage(senderLogin: String, rawMessage: String) {
        try {
            val envelope = json.decodeFromString<WsEnvelope>(rawMessage)
            
            when (envelope.type) {
                "message" -> handleNewMessage(senderLogin, envelope.payload)
                "typing" -> handleTyping(senderLogin, envelope.payload)
                "read" -> handleRead(senderLogin, envelope.payload)
                "delete" -> handleDelete(senderLogin, envelope.payload)
                "delete_chat" -> handleDeleteChat(senderLogin, envelope.payload)
                else -> println("Unknown message type: ${envelope.type}")
            }
        } catch (e: Exception) {
            println("Error handling message: ${e.message}")
        }
    }
    
    private suspend fun handleNewMessage(senderLogin: String, payload: String) {
        val messageData = json.decodeFromString<WsMessagePayload>(payload)
          
        val savedMessage = messageService.sendMessage(
            sender = senderLogin,
            receiver = messageData.receiver,
            text = messageData.text,
            replyToId = messageData.replyToId,
            clientMessageId = messageData.clientMessageId
        )

        val response = WsEnvelope(
            type = "message",
            payload = json.encodeToString(savedMessage)
        )
        val responseJson = json.encodeToString(response)

        // ✅ ИСПРАВЛЕНО: отправляем ТОЛЬКО получателю
        connectionManager.sendTo(messageData.receiver, responseJson)
        
        // ❌ УБРАЛИ: connectionManager.sendTo(senderLogin, responseJson)
        // Клиент сам добавит сообщение локально при отправке
    }
    
    private suspend fun handleTyping(senderLogin: String, payload: String) {
        val typingData = json.decodeFromString<WsTypingPayload>(payload)
        
        val response = WsEnvelope(
            type = "typing",
            payload = json.encodeToString(WsTypingPayload(
                sender = senderLogin,
                receiver = typingData.receiver,
                isTyping = typingData.isTyping
            ))
        )
        
        connectionManager.sendTo(typingData.receiver, json.encodeToString(response))
    }
    
    private suspend fun handleRead(senderLogin: String, payload: String) {
        val readData = json.decodeFromString<WsReadPayload>(payload)
        
        messageService.markAsRead(senderLogin, readData.sender)
        
        val response = WsEnvelope(
            type = "read",
            payload = json.encodeToString(WsReadPayload(
                sender = readData.sender,
                reader = senderLogin
            ))
        )
        
        connectionManager.sendTo(readData.sender, json.encodeToString(response))
    }
    
    private suspend fun handleDelete(senderLogin: String, payload: String) {
        val deleteData = json.decodeFromString<WsDeletePayload>(payload)
        
        println("🗑️ Delete request from $senderLogin: CID=${deleteData.clientMessageId}")
        
        // ✅ ИЩЕМ ТОЛЬКО ПО clientMessageId
        if (deleteData.clientMessageId.isBlank()) {
            println("❌ clientMessageId is empty!")
            return
        }
        
        val message = messageService.getMessageByClientMessageId(deleteData.clientMessageId)
        
        if (message != null) {
            // ✅ Проверяем права (только автор может удалить)
            if (message.sender != senderLogin) {
                println("❌ User $senderLogin cannot delete message from ${message.sender}")
                return
            }
            
            val deleted = messageService.deleteMessage(message.id, senderLogin)
            
            if (deleted) {
                val response = WsEnvelope(
                    type = "delete",
                    payload = json.encodeToString(
                        WsDeletePayload(
                            messageId = message.id,  // ← серверный ID (для логов)
                            clientMessageId = message.clientMessageId
                        )
                    )
                )
                val responseJson = json.encodeToString(response)
                
                // ✅ Отправляем ОБОИМ участникам
                connectionManager.sendTo(senderLogin, responseJson)
                connectionManager.sendTo(message.receiver, responseJson)
                
                println("✅ Message deleted: CID=${message.clientMessageId}")
            }
        } else {
            println("❌ Message not found: CID=${deleteData.clientMessageId}")
        }
    }
    
    private suspend fun handleDeleteChat(senderLogin: String, payload: String) {
        // ✅ ПОДДЕРЖИВАЕМ ОБА ФОРМАТА
        val data = try {
            json.decodeFromString<WsDeleteChatPayload>(payload)
        } catch (e: Exception) {
            // Fallback для старого формата {"deletedBy": "...", "chatWith": "..."}
            val map = json.decodeFromString<Map<String, String>>(payload)
            WsDeleteChatPayload(chatWith = map["chatWith"] ?: "")
        }
        
        println("🗑️ Delete chat request: $senderLogin → ${data.chatWith}")
        
        val deleted = messageService.deleteChat(senderLogin, data.chatWith)
        
        if (deleted) {
            val response = WsEnvelope(
                type = "delete_chat",
                payload = json.encodeToString(WsDeleteChatPayload(chatWith = data.chatWith))
            )
            val responseJson = json.encodeToString(response)
            
            // ✅ Уведомляем обоих
            connectionManager.sendTo(senderLogin, responseJson)
            connectionManager.sendTo(data.chatWith, responseJson)
            
            println("✅ Chat deleted between $senderLogin and ${data.chatWith}")
        }
    }
    
    private suspend fun broadcastOnlineStatus(login: String, isOnline: Boolean) {
        val response = WsEnvelope(
            type = "online",
            payload = json.encodeToString(WsOnlinePayload(login, isOnline))
        )
        val responseJson = json.encodeToString(response)
        
        println("📢 Broadcasting: $login → $isOnline")
        
        connectionManager.getOnlineUsers().forEach { user ->
            if (user != login) {
                println("  → Sending to: $user")
                connectionManager.sendTo(user, responseJson)
            }
        }
    }
}