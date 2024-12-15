package com.example.controllers

import com.example.usecases.*
import com.example.services.MockAuthService
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoutesTest {
    private lateinit var client: HttpClient
    private fun userTestApp(block: suspend () -> Unit) = testApplication {
        this@UserRoutesTest.client = createClient {
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
            userRoutes(MockUserService(), MockFollowService(), MockAuthService())
        }

        block.invoke()
    }
    @Test
    fun createUser() = userTestApp {
        val resOK = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody(validCreateUserDTO)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val res500 = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody(invalidCreateUserDTO)
        }
        assertEquals(HttpStatusCode.InternalServerError, res500.status)
    }
    @Test
    fun getUserById() = userTestApp {
        val resOK = client.get("/users/${existingUser.id}")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val notFound = client.get("/users/${newUser.id}")
        assertEquals(HttpStatusCode.NotFound, notFound.status)

        val idIsString = client.get("/users/string")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun updateUser() = userTestApp {
        val resOK = client.put("/users/${existingUser.id}") {
            contentType(ContentType.Application.Json)
            setBody(existingUser)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.put("/users/${newUser.id}") {
            contentType(ContentType.Application.Json)
            setBody(newUser)
        }
        assertEquals(HttpStatusCode.InternalServerError, doesntExist.status)

        val pathDoesntMatchDTO = client.put("/users/${newUser.id}") {
            contentType(ContentType.Application.Json)
            setBody(existingUser)
        }
        assertEquals(HttpStatusCode.BadRequest, pathDoesntMatchDTO.status)

        val idIsString = client.put("/users/string") {
            contentType(ContentType.Application.Json)
            setBody(existingUser)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun deleteUser() = userTestApp {
        val resOK = client.delete("/users/${existingUser.id}")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.delete("/users/${newUser.id}")
        assertEquals(HttpStatusCode.InternalServerError, doesntExist.status)

        val idIsString = client.delete("/users/string")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun getFollowing() = userTestApp {
        val resOK = client.get("/users/${existingUser.id}/following")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val idIsString = client.get("/users/string/following")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun createFollowing() = userTestApp {
        val resOK = client.post("/users/${newUpdateFollowingDTO.followerId}/following") {
            contentType(ContentType.Application.Json)
            setBody(newUpdateFollowingDTO)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val alreadyExists = client.post("/users/${existingUpdateFollowingDTO.followerId}/following") {
            contentType(ContentType.Application.Json)
            setBody(existingUpdateFollowingDTO)
        }
        assertEquals(HttpStatusCode.InternalServerError, alreadyExists.status)

        val pathDoesntMatchDTO = client.post("/users/${newUpdateFollowingDTO.followerId}/following") {
            contentType(ContentType.Application.Json)
            setBody(existingUpdateFollowingDTO)
        }
        assertEquals(HttpStatusCode.BadRequest, pathDoesntMatchDTO.status)

        val idIsString = client.post("/users/string/following") {
            contentType(ContentType.Application.Json)
            setBody(existingUpdateFollowingDTO)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }

    @Test
    fun deleteFollowing() = userTestApp {
        val resOK = client.delete("/users/${existingUpdateFollowingDTO.followeeId}/following")  {
            contentType(ContentType.Application.Json)
            setBody(existingUpdateFollowingDTO)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.delete("/users/${newUpdateFollowingDTO.followeeId}/following") {
            contentType(ContentType.Application.Json)
            setBody(newUpdateFollowingDTO)
        }
        assertEquals(HttpStatusCode.NotFound, doesntExist.status)

        val pathDoesntMatchDTO = client.delete("/users/${newUpdateFollowingDTO.followeeId}/following")  {
            contentType(ContentType.Application.Json)
            setBody(existingUpdateFollowingDTO)
        }
        assertEquals(HttpStatusCode.BadRequest, pathDoesntMatchDTO.status)

        val idIsString = client.delete("/users/string/following") {
            contentType(ContentType.Application.Json)
            setBody(existingUpdateFollowingDTO)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }

    @Test
    fun getFollowers() = userTestApp {
        val resOK = client.get("/users/${existingUser.id}/followers")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val idIsString = client.get("/users/string/followers")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
}