package com.messenger

import com.messenger.database.DatabaseFactory.dbQuery
import com.messenger.database.Messages
import com.messenger.database.PinnedChats
import com.messenger.database.Users
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.mindrot.jbcrypt.BCrypt
import java.time.format.DateTimeFormatter

object Storage {
    
    fun registerUser(login: String, mail: String, password: String): User? {
        return runBlocking {
            try {
                dbQuery {
                    // Проверяем что пользователя нет
                    if (Users.select { Users.login eq login }.any()) return@dbQuery null
                    
                    val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                    val userId = Users.insert {
                        it[Users.login] = login
                        it[Users.email] = mail
                        it[Users.password] = hashedPassword
                    } get Users.id
                    
                    User(userId, login, mail)
                }
            } catch (e: Exception) {
                println("Register error: ${e.message}")
                null
            }
        }
    }

    fun loginUser(login: String, password: String): User? {
        return runBlocking {
            dbQuery {
                val user = Users.select { Users.login eq login }.singleOrNull() ?: return@dbQuery null
                val hashedPassword = user[Users.password]
                if (BCrypt.checkpw(password, hashedPassword)) {
                    User(
                        user[Users.id],
                        user[Users.login],
                        user[Users.email],
                        isOnline = user[Users.isOnline]
                    )
                } else null
            }
        }
    }

    fun searchUsers(query: String, exceptLogin: String): List<User> {
        return runBlocking {
            dbQuery {
                Users.select { 
                    (Users.login like "%$query%") and (Users.login neq exceptLogin)
                }.map { row ->
                    User(
                        row[Users.id],
                        row[Users.login],
                        row[Users.email],
                        isOnline = row[Users.isOnline]
                    )
                }
            }
        }
    }

    fun addMessage(sender: String, receiver: String, text: String): Message {
        return runBlocking {
            dbQuery {
                val id = Messages.insert {
                    it[Messages.sender] = sender
                    it[Messages.receiver] = receiver
                    it[Messages.text] = text
                } get Messages.id
                
                val msg = Messages.select { Messages.id eq id }.single()
                Message(
                    id = msg[Messages.id],
                    sender = msg[Messages.sender],
                    receiver = msg[Messages.receiver],
                    text = msg[Messages.text],
                    timestamp = msg[Messages.timestamp].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    type = msg[Messages.type]
                )
            }
        }
    }

    fun getMessages(user1: String, user2: String): List<Message> {
        return runBlocking {
            dbQuery {
                Messages.select {
                    ((Messages.sender eq user1) and (Messages.receiver eq user2)) or
                    ((Messages.sender eq user2) and (Messages.receiver eq user1))
                }.orderBy(Messages.id to SortOrder.ASC).map { row ->
                    Message(
                        id = row[Messages.id],
                        sender = row[Messages.sender],
                        receiver = row[Messages.receiver],
                        text = row[Messages.text],
                        timestamp = row[Messages.timestamp].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        isRead = row[Messages.isRead],
                        type = row[Messages.type]
                    )
                }
            }
        }
    }

    fun getChatList(currentUser: String): List<ChatItem> {
        return runBlocking {
            dbQuery {
                val partners = Messages.select {
                    (Messages.sender eq currentUser) or (Messages.receiver eq currentUser)
                }.map { row ->
                    if (row[Messages.sender] == currentUser) row[Messages.receiver] else row[Messages.sender]
                }.distinct()

                partners.map { partner ->
                    val lastMsg = Messages.select {
                        ((Messages.sender eq currentUser) and (Messages.receiver eq partner)) or
                        ((Messages.sender eq partner) and (Messages.receiver eq currentUser))
                    }.orderBy(Messages.id to SortOrder.DESC).limit(1).firstOrNull()

                    ChatItem(
                        name = partner,
                        lastMessage = lastMsg?.get(Messages.text) ?: "",
                        lastMessageTime = lastMsg?.get(Messages.timestamp)?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "",
                        unreadCount = 0
                    )
                }
            }
        }
    }
}