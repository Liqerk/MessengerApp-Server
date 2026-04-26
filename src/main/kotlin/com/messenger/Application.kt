package com.messenger

import com.messenger.config.AppConfig
import com.messenger.domain.repository.*
import com.messenger.domain.service.*
import com.messenger.infrastructure.database.DatabaseFactory
import com.messenger.infrastructure.database.ExposedUserRepository
import com.messenger.infrastructure.database.ExposedMessageRepository
import com.messenger.infrastructure.database.ExposedChatRepository
import com.messenger.infrastructure.websocket.ConnectionManager
import com.messenger.presentation.route.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import io.ktor.util.AttributeKey

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long
)

@Serializable
data class ErrorResponse(
    val error: String
)

fun Application.module(config: AppConfig) {
    DatabaseFactory.init(config.database)
    attributes.put(JwtConfigKey, config.jwt)
    
    val userRepository: UserRepository = ExposedUserRepository()
    val messageRepository: MessageRepository = ExposedMessageRepository()
    val chatRepository: ChatRepository = ExposedChatRepository()
    
    val authService = AuthService(userRepository, config.jwt)
    val userService = UserService(userRepository)
    val messageService = MessageService(messageRepository, userRepository)
    val chatService = ChatService(chatRepository, messageRepository)
    val webSocketService = WebSocketService(
        connectionManager = ConnectionManager,
        messageService = messageService,
        userService = userService
    )
    
    install(DefaultHeaders)
    install(CallLogging) { level = Level.INFO }
    
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
    }
    
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid request"))
        }
        exception<Throwable> { call, cause ->
            println("❌ Error: ${cause.message}")
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal error"))
        }
    }
    
    install(Authentication) {
        jwt("auth-jwt") {
            realm = config.jwt.realm
            verifier {
                com.auth0.jwt.JWT.require(
                    com.auth0.jwt.algorithms.Algorithm.HMAC256(config.jwt.secret)
                )
                    .withIssuer(config.jwt.issuer)
                    .withAudience(config.jwt.audience)
                    .build()
            }
            validate { credential ->
                val login = credential.payload.getClaim("login").asString()
                if (login != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
            }
        }
    }
    
    install(WebSockets) {
        pingPeriod = java.time.Duration.ofSeconds(15)
        timeout = java.time.Duration.ofSeconds(30)
        maxFrameSize = 1024 * 1024
        masking = false
    }
    
    routing {
        get("/health") {
            call.respond(HealthResponse("ok", System.currentTimeMillis()))
        }
        
        fileUploadRoute()
        legacyRoutes(authService, userService, messageService, chatService)
        legacyWebSocketRoute(webSocketService, messageService)
        
        route("/api/v1") {
            authRoutes(authService)
        }
        
        authenticate("auth-jwt") {
            route("/api/v1") {
                userRoutes(userService)
                messageRoutes(messageService)
                chatRoutes(chatService)
            }
        }
    }
    
    println("🚀 Server started on ${config.server.host}:${config.server.port}")
}

val JwtConfigKey = AttributeKey<com.messenger.config.JwtConfig>("JwtConfig")
