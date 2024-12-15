package com.example.controllers

import com.example.usecases.MockUserService
import com.example.usecases.existingUser
import com.example.usecases.newUser
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthRoutesTest {
    private lateinit var client: HttpClient
    private fun authTestApp(block: suspend () -> Unit) = testApplication {
        this@AuthRoutesTest.client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }
        application {
            install(ContentNegotiation) {
                json()
            }
        }
        routing {
            authRoutes(MockUserService())
        }

        block.invoke()
    }
    @Test
    fun login() = authTestApp {
        client.get("/auth/login/${existingUser.authId}").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get("/auth/login/${newUser.authId}").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertEquals("User not found", bodyAsText())
        }
    }
}