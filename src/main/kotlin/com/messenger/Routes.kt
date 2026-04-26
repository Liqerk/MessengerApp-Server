package com.messenger

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

val connections = ConcurrentHashMap<String, DefaultWebSocketSession>()

fun Route.authRoutes() {
    route("/auth") {
        post("/register") {
            try {
                val req = call.receive<RegisterRequest>()
                val user = Storage.registerUser(req.login, req.email, req.password)
                if (user != null) {
                    val token = "jwt-${user.login}-${System.currentTimeMillis()}"
                    call.respond(AuthResponse(token, user.copy(password = "")))
                    println("✅ Registered: ${user.login}")
                } else {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "User already exists"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }

        post("/login") {
            try {
                val req = call.receive<LoginRequest>()
                val user = Storage.loginUser(req.login, req.password)
                if (user != null) {
                    val token = "jwt-${user.login}-${System.currentTimeMillis()}"
                    call.respond(AuthResponse(token, user.copy(password = "")))
                    println("✅ Login: ${user.login}")
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
    }
}

fun Route.userRoutes() {
    get("/users/search") {
        val query = call.request.queryParameters["q"] ?: ""
        val currentUser = call.request.queryParameters["except"] ?: ""
        val users = Storage.searchUsers(query, currentUser).map { it.copy(password = "") }
        call.respond(users)
    }
}

fun Route.messageRoutes() {
    get("/messages") {
        val user1 = call.request.queryParameters["user1"] ?: ""
        val user2 = call.request.queryParameters["user2"] ?: ""
        val messages = Storage.getMessages(user1, user2)
        call.respond(messages)
    }

    get("/chats") {
        val user = call.request.queryParameters["user"] ?: ""
        val chats = Storage.getChatList(user)
        call.respond(chats)
    }
}

fun Route.webSocketRoute() {
    webSocket("/ws") {
        val token = call.request.queryParameters["token"] ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No token"))
            return@webSocket
        }

        // Простой парсинг токена (jwt-login-timestamp)
        val login = token.split("-").getOrNull(1) ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }

        connections[login] = this
        println("✅ WebSocket connected: $login")

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val msg = Json.decodeFromString<Message>(text)

                    // Сохраняем в "БД"
                    Storage.addMessage(msg.sender, msg.receiver, msg.text)

                    // Отправляем получателю (если онлайн)
                    connections[msg.receiver]?.send(Frame.Text(text))
                    
                    println("📨 ${msg.sender} → ${msg.receiver}: ${msg.text}")
                }
            }
        } catch (e: Exception) {
            println("❌ WebSocket error: ${e.message}")
        } finally {
            connections.remove(login)
            println("❌ WebSocket disconnected: $login")
        }
    }
}
