package com.messenger.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val rateLimit: RateLimitConfig
) {
    companion object {
        fun load(): AppConfig {
            val env = System.getenv()
            
            return AppConfig(
                server = ServerConfig(
                    host = env["SERVER_HOST"] ?: "0.0.0.0",
                    port = env["SERVER_PORT"]?.toIntOrNull() ?: 8080,
                    environment = env["ENVIRONMENT"] ?: "development"
                ),
                database = DatabaseConfig(
                    url = env["DB_URL"] ?: "jdbc:postgresql://localhost:5432/messenger",
                    driver = env["DB_DRIVER"] ?: "org.postgresql.Driver",
                    user = env["DB_USER"] ?: "messenger",
                    password = env["DB_PASSWORD"] ?: "password",
                    maxPoolSize = env["DB_MAX_POOL_SIZE"]?.toIntOrNull() ?: 10,
                    connectionTimeout = env["DB_CONNECTION_TIMEOUT"]?.toLongOrNull() ?: 30000
                ),
                jwt = JwtConfig(
                    secret = env["JWT_SECRET"] ?: throw IllegalArgumentException("JWT_SECRET is required"),
                    issuer = env["JWT_ISSUER"] ?: "messenger-server",
                    audience = env["JWT_AUDIENCE"] ?: "messenger-client",
                    realm = env["JWT_REALM"] ?: "messenger",
                    expirationDays = env["JWT_EXPIRATION_DAYS"]?.toIntOrNull() ?: 7
                ),
                rateLimit = RateLimitConfig(
                    enabled = env["RATE_LIMIT_ENABLED"]?.toBoolean() ?: true,
                    requests = env["RATE_LIMIT_REQUESTS"]?.toIntOrNull() ?: 100,
                    period = env["RATE_LIMIT_PERIOD"]?.toLongOrNull() ?: 60000
                )
            )
        }
    }
}

@Serializable
data class ServerConfig(
    val host: String,
    val port: Int,
    val environment: String
)

@Serializable
data class DatabaseConfig(
    val url: String,
    val driver: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
    val connectionTimeout: Long
)

@Serializable
data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expirationDays: Int
)

@Serializable
data class RateLimitConfig(
    val enabled: Boolean,
    val requests: Int,
    val period: Long
)
