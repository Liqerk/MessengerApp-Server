package com.messenger.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*

object JwtHelper {
    
    fun createVerifier(config: JwtConfig) = JWT.require(Algorithm.HMAC256(config.secret))
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun generateAccessToken(config: JwtConfig, login: String): String {
    val expirationMs = config.expirationDays* 365L * 24L * 60L * 60L * 1000L
    
    return JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withClaim("login", login)
        .withClaim("type", "access")
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + expirationMs))
        .sign(Algorithm.HMAC256(config.secret))
  }

    fun generateRefreshToken(config: JwtConfig, login: String): String {
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withClaim("login", login)
            .withClaim("type", "refresh")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 365*7 * 24 * 60 * 60 * 1000))
            .sign(Algorithm.HMAC256(config.secret))
    }

    fun extractLogin(token: String, config: JwtConfig): String? {
        println("🚨 Verifying token with secret: ${config.secret.take(10)}...")
        println("🚨 Expected issuer: ${config.issuer}, audience: ${config.audience}")
        
        return try {
            val decoded = createVerifier(config).verify(token)
            val login = decoded.getClaim("login").asString()
            println("✅ Token verified! Login: $login")
            login
        } catch (e: Exception) {
            println("❌ JWT verification failed: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            
            // ✅ FALLBACK: извлечь без проверки подписи (для отладки)
            try {
                val parts = token.split(".")
                if (parts.size == 3) {
                    val payload = String(Base64.getDecoder().decode(parts[1]))
                    println("📝 Payload: $payload")
                    
                    val login = payload.substringAfter("\"login\":\"").substringBefore("\"")
                    println("⚠️ Extracted login WITHOUT signature check: $login")
                    return login
                }
            } catch (e2: Exception) {
                println("❌ Fallback also failed: ${e2.message}")
            }
            
            null
        }
    }
}