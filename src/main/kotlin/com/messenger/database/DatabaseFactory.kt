package com.messenger.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    
    fun init() {
        val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/messenger"
        val dbUser = System.getenv("DB_USER") ?: "messenger"
        val dbPassword = System.getenv("DB_PASSWORD") ?: "password"
        
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        
        Database.connect(HikariDataSource(config))
        
        transaction {
            SchemaUtils.create(Users, Messages, PinnedChats)
        }
        
        println("✅ PostgreSQL connected: $dbUrl")
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}