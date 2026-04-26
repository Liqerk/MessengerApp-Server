package com.messenger.domain.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.messenger.config.JwtConfig
import com.messenger.config.JwtHelper
import com.messenger.domain.model.User
import com.messenger.domain.repository.UserRepository
import com.messenger.presentation.dto.request.LoginRequest
import com.messenger.presentation.dto.request.RegisterRequest
import com.messenger.presentation.dto.response.AuthResponse

class AuthService(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig
) {
    
    private val bcryptHasher = BCrypt.withDefaults()
    private val bcryptVerifier = BCrypt.verifyer()
    
    suspend fun register(request: RegisterRequest): AuthResponse {
        require(request.login.isNotBlank()) { "Login cannot be empty" }
        require(request.login.length in 3..50) { "Login must be 3-50 characters" }
        require(request.email.isNotBlank()) { "Email cannot be empty" }
        require(request.password.length >= 4) { "Password must be at least 4 characters" }
        
        if (userRepository.existsByLogin(request.login)) {
            throw IllegalArgumentException("User with this login already exists")
        }
        
        val passwordHash = bcryptHasher.hashToString(12, request.password.toCharArray())
        
        val user = User(
            login = request.login,
            email = request.email,
            displayName = request.displayName ?: request.login
        )
        
        val createdUser = userRepository.create(user, passwordHash)
        
        val accessToken = JwtHelper.generateAccessToken(jwtConfig, createdUser.login)
        val refreshToken = JwtHelper.generateRefreshToken(jwtConfig, createdUser.login)
        
        return AuthResponse(
            user = createdUser,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
    
    suspend fun login(request: LoginRequest): AuthResponse {
        require(request.login.isNotBlank()) { "Login cannot be empty" }
        require(request.password.isNotBlank()) { "Password cannot be empty" }
        
        val user = userRepository.findByLogin(request.login)
            ?: throw IllegalArgumentException("Invalid credentials")
        
        val result = bcryptVerifier.verify(
            request.password.toCharArray(),
            user.passwordHash ?: ""
        )
        
        if (!result.verified) {
            throw IllegalArgumentException("Invalid credentials")
        }
        
        userRepository.updateOnlineStatus(user.login, true)
        
        val accessToken = JwtHelper.generateAccessToken(jwtConfig, user.login)
        val refreshToken = JwtHelper.generateRefreshToken(jwtConfig, user.login)
        
        return AuthResponse(
            user = user.copy(isOnline = true, passwordHash = null),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
    
    suspend fun logout(login: String) {
        userRepository.updateOnlineStatus(login, false)
    }
    
    fun validateToken(token: String): String? {
        return JwtHelper.extractLogin(token, jwtConfig)
    }
}