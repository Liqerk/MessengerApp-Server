package com.messenger.presentation.route

import com.messenger.config.JwtConfig
import com.messenger.config.JwtHelper
import com.messenger.domain.service.WebSocketService
import com.messenger.infrastructure.websocket.ConnectionManager
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun Route.webSocketRoute(webSocketService: WebSocketService) {
    webSocket("/ws") {
        val token = call.request.queryParameters["token"]
        
        if (token == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No token provided"))
            return@webSocket
        }
        
        val jwtConfig = call.application.attributes.getOrNull(JwtConfigKey)
        
        if (jwtConfig == null) {
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Server configuration error"))
            return@webSocket
        }
        
        val login = JwtHelper.extractLogin(token, jwtConfig)
        
        if (login == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid or expired token"))
            return@webSocket
        }
        
        // ✅ ДОБАВЛЯЕМ СЕССИЮ
        ConnectionManager.addSession(login, this)
        
        // ✅ СРАЗУ РАССЫЛАЕМ СТАТУС (ДО handleConnection)
        webSocketService.handleConnection(login)
        
        try {
            incoming.consumeEach { frame ->
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        webSocketService.handleMessage(login, text)
                    }
                    is Frame.Ping -> send(Frame.Pong(frame.data))
                    else -> {}
                }
            }
        } catch (e: Exception) {
            println("WebSocket error for $login: ${e.message}")
        } finally {
            ConnectionManager.removeSession(login, this)
            webSocketService.handleDisconnection(login)
        }
    }
}

// Ключ для хранения конфига в атрибутах Application
val JwtConfigKey = io.ktor.util.AttributeKey<JwtConfig>("JwtConfig")
