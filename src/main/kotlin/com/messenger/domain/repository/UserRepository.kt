package com.messenger.domain.repository

import com.messenger.domain.model.User

interface UserRepository {
    suspend fun create(user: User, passwordHash: String): User
    suspend fun findByLogin(login: String): User?
    suspend fun findByEmail(email: String): User?
    suspend fun search(query: String, excludeLogin: String, limit: Int): List<User>
    suspend fun update(user: User): User
    suspend fun updateOnlineStatus(login: String, isOnline: Boolean)
    suspend fun updateLastSeen(login: String)
    suspend fun existsByLogin(login: String): Boolean
    suspend fun existsByEmail(email: String): Boolean
}
