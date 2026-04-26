package com.messenger.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class User(
    val id: Int = 0,
    val login: String,
    val email: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    @kotlinx.serialization.Transient
    val lastSeen: Instant? = null,
    @kotlinx.serialization.Transient
    val createdAt: Instant? = null,
    @kotlinx.serialization.Transient
    val updatedAt: Instant? = null,
    @kotlinx.serialization.Transient
    val passwordHash: String? = null
) {
    fun toPublic() = copy(passwordHash = null)
}
