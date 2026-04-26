package com.messenger.presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun <T> success(data: T) = ApiResponse(success = true, data = data)
        fun error(message: String) = ApiResponse<Unit>(success = false, error = message)
    }
}
