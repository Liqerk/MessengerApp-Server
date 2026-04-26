package com.messenger.domain.service

import com.messenger.domain.model.User
import com.messenger.domain.repository.UserRepository

class UserService(
    private val userRepository: UserRepository
) {
    
    suspend fun search(query: String, currentUser: String, limit: Int = 50): List<User> {
        require(query.isNotBlank()) { "Search query cannot be empty" }
        val safeLimit = limit.coerceIn(1, 100)
        
        return userRepository.search(query, currentUser, safeLimit)
            .map { it.toPublic() }
    }
    
    suspend fun getByLogin(login: String): User? {
        return userRepository.findByLogin(login)?.toPublic()
    }
    
    suspend fun updateProfile(login: String, displayName: String?, avatarUrl: String?): User {
        val user = userRepository.findByLogin(login)
            ?: throw IllegalArgumentException("User not found")
        
        val updatedUser = user.copy(
            displayName = displayName ?: user.displayName,
            avatarUrl = avatarUrl ?: user.avatarUrl
        )
        
        return userRepository.update(updatedUser).toPublic()
    }
    
    suspend fun setOnlineStatus(login: String, isOnline: Boolean) {
        userRepository.updateOnlineStatus(login, isOnline)
    }
    
    suspend fun updateLastSeen(login: String) {
        userRepository.updateLastSeen(login)
    }
}
