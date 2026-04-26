package com.messenger.presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val email: String, 
    val password: String,
    val displayName: String? = null
) 