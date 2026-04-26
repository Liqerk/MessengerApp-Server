package com.messenger.infrastructure.websocket

import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object ConnectionManager {
    
    private val sessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()
    private val mutex = Mutex()
    
    suspend fun addSession(login: String, session: WebSocketSession) {
        mutex.withLock {
            sessions.getOrPut(login) { ConcurrentHashMap.newKeySet() }.add(session)
        }
        println("✅ WebSocket connected: $login (total sessions: ${sessions[login]?.size})")
    }
    
    suspend fun removeSession(login: String, session: WebSocketSession) {
        mutex.withLock {
            sessions[login]?.remove(session)
            if (sessions[login]?.isEmpty() == true) {
                sessions.remove(login)
            }
        }
        println("❌ WebSocket disconnected: $login (remaining: ${sessions[login]?.size ?: 0})")
    }
    
    fun isOnline(login: String): Boolean {
        return sessions[login]?.isNotEmpty() == true
    }
    
    // ✅ НОВЫЙ МЕТОД: Получить все сессии пользователя
    fun getSessions(login: String): Set<WebSocketSession> {
        return sessions[login]?.toSet() ?: emptySet()
    }
    
    suspend fun sendTo(login: String, message: String) {
        sessions[login]?.forEach { session ->
            try {
                if (!session.outgoing.isClosedForSend) {
                    session.send(Frame.Text(message))
                }
            } catch (e: Exception) {
                println("Error sending to $login: ${e.message}")
            }
        }
    }
    
    suspend fun broadcast(message: String, exclude: String? = null) {
        sessions.keys.forEach { login ->
            if (login != exclude) {
                sendTo(login, message)
            }
        }
    }
    
    fun getOnlineUsers(): Set<String> {
        return sessions.keys.toSet()
    }
    
    fun getSessionCount(login: String): Int {
        return sessions[login]?.size ?: 0
    }
    
    fun getTotalConnections(): Int {
        return sessions.values.sumOf { it.size }
    }
}