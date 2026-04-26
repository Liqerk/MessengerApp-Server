package com.messenger.presentation.route

import com.messenger.domain.service.UserService
import com.messenger.presentation.dto.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.userRoutes(userService: UserService) {
    route("/users") {
        
        get("/search") {
            try {
                val query = call.request.queryParameters["q"] ?: ""
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                
                val principal = call.principal<JWTPrincipal>()
                val currentUser = principal?.payload?.getClaim("login")?.asString() ?: ""
                
                val users = userService.search(query, currentUser, limit)
                call.respond(HttpStatusCode.OK, ApiResponse.success(users))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Invalid request"))
            }
        }
        
        get("/me") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val login = principal?.payload?.getClaim("login")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val user = userService.getByLogin(login)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.error("User not found"))
                
                call.respond(HttpStatusCode.OK, ApiResponse.success(user))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Failed to get user"))
            }
        }
        
        get("/{login}") {
            try {
                val login = call.parameters["login"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Login required"))
                
                val user = userService.getByLogin(login)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.error("User not found"))
                
                call.respond(HttpStatusCode.OK, ApiResponse.success(user))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Failed to get user"))
            }
        }
        
        put("/me") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val login = principal?.payload?.getClaim("login")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiResponse.error("Unauthorized"))
                
                val request = call.receive<UpdateProfileRequest>()
                val user = userService.updateProfile(login, request.displayName, request.avatarUrl)
                
                call.respond(HttpStatusCode.OK, ApiResponse.success(user))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Invalid request"))
            }
        }
    }
}

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null
)
