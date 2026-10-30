package ru.ithub.tasks.security

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import ru.ithub.tasks.model.UnauthorizedException
import ru.ithub.tasks.storage.UserRepository

fun Application.configureAuthentication(jwt: JwtService, users: UserRepository) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwt.settings.realm
            verifier(jwt.verifier())
            validate { credential ->
                val userId = runCatching { credential.payload.getClaim("userId").asInt() }.getOrNull()
                if (userId != null && users.findById(userId) != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                throw UnauthorizedException("Нужен действительный токен в заголовке Authorization: Bearer")
            }
        }
    }
}
