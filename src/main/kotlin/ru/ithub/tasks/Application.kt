package ru.ithub.tasks

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import ru.ithub.tasks.plugins.configureRequestLogging
import ru.ithub.tasks.plugins.configureStatusPages
import ru.ithub.tasks.routes.configureRouting
import ru.ithub.tasks.security.AuthService
import ru.ithub.tasks.security.JwtService
import ru.ithub.tasks.security.JwtSettings
import ru.ithub.tasks.security.PasswordHasher
import ru.ithub.tasks.security.configureAuthentication
import ru.ithub.tasks.storage.TaskRepository
import ru.ithub.tasks.storage.UserRepository

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 8080
    embeddedServer(Netty, port = port, host = "127.0.0.1") {
        module()
    }.start(wait = true)
}

fun Application.module(
    taskRepository: TaskRepository = TaskRepository.withSamples(),
    userRepository: UserRepository = UserRepository(),
    passwordHasher: PasswordHasher = PasswordHasher(),
    jwtSettings: JwtSettings = JwtSettings(),
) {
    val jwt = JwtService(jwtSettings)
    val auth = AuthService(userRepository, passwordHasher, jwt)
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            },
        )
    }
    configureStatusPages()
    configureRequestLogging()
    configureAuthentication(jwt, userRepository)
    configureRouting(taskRepository, auth)
    log.info("Маршруты задач, Swagger и журнал запросов зарегистрированы")
}
