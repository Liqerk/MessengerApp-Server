package com.messenger.infrastructure.database

import com.messenger.domain.model.User
import com.messenger.domain.repository.UserRepository
import com.messenger.infrastructure.database.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import java.time.Instant

class ExposedUserRepository : UserRepository {
    
    override suspend fun create(user: User, passwordHash: String): User = dbQuery {
        val id = Users.insert {
            it[login] = user.login
            it[email] = user.email
            it[Users.passwordHash] = passwordHash // ← FIX
            it[displayName] = user.displayName
            it[avatarUrl] = user.avatarUrl
            it[createdAt] = Instant.now()
            it[updatedAt] = Instant.now()
        } get Users.id
        
        user.copy(id = id)
    }
    
    override suspend fun findByLogin(login: String): User? = dbQuery {
        Users.select { Users.login eq login }
            .map { it.toUser() }
            .singleOrNull()
    }
    
    override suspend fun findByEmail(email: String): User? = dbQuery {
        Users.select { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }
    
    override suspend fun search(query: String, excludeLogin: String, limit: Int): List<User> = dbQuery {
        val safeQuery = "%${query.replace("%", "\\%").replace("_", "\\_")}%"
        
        Users.select {
            ((Users.login like safeQuery) or (Users.displayName like safeQuery)) and
            (Users.login neq excludeLogin)
        }
        .limit(limit)
        .map { it.toUser() }
    }
    
    override suspend fun update(user: User): User = dbQuery {
        Users.update({ Users.id eq user.id }) {
            it[displayName] = user.displayName
            it[avatarUrl] = user.avatarUrl
            it[updatedAt] = Instant.now()
        }
        user
    }
    
    override suspend fun updateOnlineStatus(login: String, isOnline: Boolean) { // ← FIX (убрали = dbQuery)
        dbQuery {
            Users.update({ Users.login eq login }) {
                it[Users.isOnline] = isOnline
                if (!isOnline) {
                    it[lastSeen] = Instant.now()
                }
                it[updatedAt] = Instant.now()
            }
        }
    }
    
    override suspend fun updateLastSeen(login: String) { // ← FIX
        dbQuery {
            Users.update({ Users.login eq login }) {
                it[lastSeen] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
    }
    
    override suspend fun existsByLogin(login: String): Boolean = dbQuery {
        Users.select { Users.login eq login }.count() > 0
    }
    
    override suspend fun existsByEmail(email: String): Boolean = dbQuery {
        Users.select { Users.email eq email }.count() > 0
    }
    
    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        login = this[Users.login],
        email = this[Users.email],
        displayName = this[Users.displayName],
        avatarUrl = this[Users.avatarUrl],
        isOnline = this[Users.isOnline],
        lastSeen = this[Users.lastSeen],
        createdAt = this[Users.createdAt],
        updatedAt = this[Users.updatedAt],
        passwordHash = this[Users.passwordHash]
    )
}
