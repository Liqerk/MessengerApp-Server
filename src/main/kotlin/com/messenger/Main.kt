package com.messenger

import com.messenger.config.AppConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    // Загрузка конфигурации
    val config = AppConfig.load()
    
    // Запуск сервера
    embeddedServer(
        factory = Netty,
        port = config.server.port,
        host = config.server.host,
        module = { module(config) }
    ).start(wait = true)
}
