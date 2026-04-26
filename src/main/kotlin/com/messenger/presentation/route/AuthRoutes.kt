package com.messenger.presentation.route

import com.messenger.domain.service.AuthService
import com.messenger.presentation.dto.request.LoginRequest
import com.messenger.presentation.dto.request.RegisterRequest
import com.messenger.presentation.dto.response.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        
        post("/register") {
            try {
                // ✅ Читаем сырой JSON
                val rawBody = call.receiveText()
                println("📥 RAW JSON received: $rawBody")
                
                // ✅ Парсим вручную для отладки
                val request = Json { ignoreUnknownKeys = true }.decodeFromString<RegisterRequest>(rawBody)
                println("✅ Parsed RegisterRequest: login=${request.login}, email=${request.email}")
                
                val response = authService.register(request)
                println("✅ Registration successful for: ${request.login}")
                
                call.respond(HttpStatusCode.Created, ApiResponse.success(response))
            } catch (e: SerializationException) {
                println("❌ JSON parsing error: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Invalid JSON format: ${e.message}"))
            } catch (e: IllegalArgumentException) {
                println("❌ Validation error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error(e.message ?: "Invalid request"))
            } catch (e: Exception) {
                println("❌ Unexpected error: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Registration failed"))
            }
        }
        
        post("/login") {
            try {
                val rawBody = call.receiveText()
                println("📥 RAW JSON received: $rawBody")
                
                val request = Json { ignoreUnknownKeys = true }.decodeFromString<LoginRequest>(rawBody)
                println("✅ Parsed LoginRequest: login=${request.login}")
                
                val response = authService.login(request)
                println("✅ Login successful for: ${request.login}")
                
                call.respond(HttpStatusCode.OK, ApiResponse.success(response))
            } catch (e: SerializationException) {
                println("❌ JSON parsing error: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error("Invalid JSON format"))
            } catch (e: IllegalArgumentException) {
                println("❌ Login error: ${e.message}")
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error(e.message ?: "Invalid credentials"))
            } catch (e: Exception) {
                println("❌ Unexpected error: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.error("Login failed"))
            }
        }
    }
}