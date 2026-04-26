package com.messenger.presentation.dto.response

import com.messenger.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String
)
