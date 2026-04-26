package com.messenger.presentation.route

import com.messenger.domain.service.ChatService
import com.messenger.presentation.dto.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes(chatService: ChatService) {
    route("/chats") {
        
        get {
            try {
                val principal = call.principal<JWTPrincipal>()
                val currentUser = principal?.payload?.getClaim("login")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val chats = chatService.getChatList(currentUser)
                call.respond(HttpStatusCode.OK, ApiResponse.success(chats))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Failed to get chats"))
            }
        }
        
        post("/{partner}/pin") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val currentUser = principal?.payload?.getClaim("login")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val partner = call.parameters["partner"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Partner required"))
                
                val isPinned = chatService.togglePinChat(currentUser, partner)
                call.respond(HttpStatusCode.OK, ApiResponse.success(mapOf("isPinned" to isPinned)))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Failed to pin chat"))
            }
        }
    }
}
