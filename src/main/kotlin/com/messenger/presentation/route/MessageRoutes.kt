package com.messenger.presentation.route

import com.messenger.domain.service.MessageService
import com.messenger.presentation.dto.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.messageRoutes(messageService: MessageService) {
    route("/messages") {
        
        get("/{partner}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val currentUser = principal?.payload?.getClaim("login")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val partner = call.parameters["partner"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Partner required"))
                
                val beforeId = call.request.queryParameters["before"]?.toIntOrNull()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                
                val messages = messageService.getMessages(currentUser, partner, beforeId, limit)
                call.respond(HttpStatusCode.OK, ApiResponse.success(messages))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Failed to get messages"))
            }
        }
        
        post("/") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val sender = principal?.payload?.getClaim("login")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val request = call.receive<SendMessageRequest>()
                
                val message = messageService.sendMessage(
                    sender = sender,
                    receiver = request.receiver,
                    text = request.text,
                    replyToId = request.replyToId,
                    clientMessageId = request.clientMessageId
                )
                
                call.respond(HttpStatusCode.Created, ApiResponse.success(message))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Invalid request"))
            }
        }
        
        post("/{id}/read") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val reader = principal?.payload?.getClaim("login")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val sender = call.request.queryParameters["sender"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Sender required"))
                
                messageService.markAsRead(reader, sender)
                call.respond(HttpStatusCode.OK, ApiResponse.success("Messages marked as read"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Failed to mark as read"))
            }
        }
        
        delete("/{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val deletedBy = principal?.payload?.getClaim("login")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val messageId = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Message ID required"))
                
                val deleted = messageService.deleteMessage(messageId, deletedBy)
                
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse.success("Message deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Message not found or cannot be deleted"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Failed to delete message"))
            }
        }
        
        put("/{id}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val editedBy = principal?.payload?.getClaim("login")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val messageId = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Message ID required"))
                
                val request = call.receive<EditMessageRequest>()
                val message = messageService.editMessage(messageId, request.text, editedBy)
                
                call.respond(HttpStatusCode.OK, ApiResponse.success(message))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Invalid request"))
            }
        }
        
        post("/{id}/favorite") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("login")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val messageId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Message ID required"))
                
                val isFavorite = messageService.toggleFavorite(messageId, userId)
                call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("isFavorite" to isFavorite)))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Failed to toggle favorite"))
            }
        }
    }
}

@Serializable
data class SendMessageRequest(
    val receiver: String,
    val text: String,
    val replyToId: Int? = null,
    val clientMessageId: String = "" 
)

@Serializable
data class EditMessageRequest(
    val text: String
)
