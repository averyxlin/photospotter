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

class PinRoutesTest {
    private lateinit var client: HttpClient
    private fun pinTestApp(block: suspend () -> Unit) = testApplication {
        this@PinRoutesTest.client = createClient {
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
            pinRoutes(
                MockBookmarkService(),
                MockCommentService(),
                MockLikeService(),
                MockPinService(),
                MockTagService(),
                MockAuthService(),
            )
        }

        block.invoke()
    }
    @Test
    fun getAllPins() = pinTestApp {
        val resOK = client.get("/pins")
        assertEquals(HttpStatusCode.OK, resOK.status)
    }
    @Test
    fun createPin() = pinTestApp {
        val resOK = client.post("/pins") {
            contentType(ContentType.Application.Json)
            setBody(validCreatePinDTO)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val invalid = client.post("/pins") {
            contentType(ContentType.Application.Json)
            setBody(invalidCreatePinDTO)
        }
        assertEquals(HttpStatusCode.InternalServerError, invalid.status)
    }
    @Test
    fun getTagOptions() = pinTestApp {
        val resOK = client.get("/pins/tags/options")
        assertEquals(HttpStatusCode.OK, resOK.status)
    }
    @Test
    fun getPinById() = pinTestApp {
        val resOK = client.get("/pins/${existingPin.id}")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val notFound = client.get("/pins/${newPin.id}")
        assertEquals(HttpStatusCode.NotFound, notFound.status)

        val idIsString = client.get("/pins/string")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun updatePin() = pinTestApp {
        val resOK = client.put("/pins/${existingPin.id}") {
            contentType(ContentType.Application.Json)
            setBody(existingPin)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.put("/pins/${newPin.id}") {
            contentType(ContentType.Application.Json)
            setBody(newPin)
        }
        assertEquals(HttpStatusCode.InternalServerError, doesntExist.status)

        val pathDoesntMatchDTO = client.put("/pins/${existingPin.id}") {
            contentType(ContentType.Application.Json)
            setBody(newPin)
        }
        assertEquals(HttpStatusCode.BadRequest, pathDoesntMatchDTO.status)

        val idIsString = client.put("/pins/string") {
            contentType(ContentType.Application.Json)
            setBody(newPin)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun deletePin() = pinTestApp {
        val resOK = client.delete("/pins/${existingPin.id}")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.delete("/pins/${newPin.id}")
        assertEquals(HttpStatusCode.NotFound, doesntExist.status)

        val idIsString = client.delete("/pins/string")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun createComment() = pinTestApp {
        val resOK = client.post("/pins/${existingPin.id}/comments") {
            contentType(ContentType.Application.Json)
            setBody(validCreateCommentDTO)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.post("/pins/${newPin.id}/comments") {
            contentType(ContentType.Application.Json)
            setBody(invalidCreateCommentDTO)
        }
        assertEquals(HttpStatusCode.InternalServerError, doesntExist.status)

        val pathDoesntMatchDTO = client.post("/pins/${existingPin.id}/comments") {
            contentType(ContentType.Application.Json)
            setBody(invalidCreateCommentDTO)
        }
        assertEquals(HttpStatusCode.BadRequest, pathDoesntMatchDTO.status)

        val idIsString = client.post("/pins/string/comments") {
            contentType(ContentType.Application.Json)
            setBody(validCreateCommentDTO)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun deleteComment() = pinTestApp {
        val resOK = client.delete("/pins/${existingPin.id}/comments/${existingComment.id}")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.delete("/pins/${existingPin.id}/comments/${newComment.id}")
        assertEquals(HttpStatusCode.NotFound, doesntExist.status)

        val idIsString = client.delete("/pins/${newPin.id}/comments/string")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun createTags() = pinTestApp {
        val resOK = client.post("/pins/${existingPin.id}/tags") {
            contentType(ContentType.Application.Json)
            setBody(validCreateTagDTO)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.post("/pins/${newPin.id}/tags") {
            contentType(ContentType.Application.Json)
            setBody(invalidCreateTagDTO)
        }
        assertEquals(HttpStatusCode.InternalServerError, doesntExist.status)

        val pathDoesntMatchDTO = client.post("/pins/${existingPin.id}/tags") {
            contentType(ContentType.Application.Json)
            setBody(invalidCreateTagDTO)
        }
        assertEquals(HttpStatusCode.BadRequest, pathDoesntMatchDTO.status)

        val idIsString = client.post("/pins/string/tags") {
            contentType(ContentType.Application.Json)
            setBody(validCreateCommentDTO)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun deleteTag() = pinTestApp {
        val resOK = client.delete("/pins/${existingPin.id}/tags/${existingTag.tagId}")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.delete("/pins/${existingPin.id}/tags/${newTag.tagId}")
        assertEquals(HttpStatusCode.NotFound, doesntExist.status)

        val tagIdIsString = client.delete("/pins/${newPin.id}/tags/string")
        assertEquals(HttpStatusCode.BadRequest, tagIdIsString.status)

        val pinIdIsString = client.delete("/pins/string/tags/${existingTag.tagId}")
        assertEquals(HttpStatusCode.BadRequest, pinIdIsString.status)
    }
    @Test
    fun createLike() = pinTestApp {
        val resOK = client.post("/pins/${existingPin.id}/likes") {
            contentType(ContentType.Application.Json)
            setBody(validLike)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.post("/pins/${newPin.id}/likes") {
            contentType(ContentType.Application.Json)
            setBody(invalidLike)
        }
        assertEquals(HttpStatusCode.InternalServerError, doesntExist.status)

        val pathDoesntMatchDTO = client.post("/pins/${existingPin.id}/likes") {
            contentType(ContentType.Application.Json)
            setBody(invalidLike)
        }
        assertEquals(HttpStatusCode.BadRequest, pathDoesntMatchDTO.status)

        val idIsString = client.post("/pins/string/likes") {
            contentType(ContentType.Application.Json)
            setBody(validLike)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun deleteLike() = pinTestApp {
        val resOK = client.delete("/pins/${existingPin.id}/likes") {
            contentType(ContentType.Application.Json)
            setBody(validLike)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.delete("/pins/${newPin.id}/likes") {
            contentType(ContentType.Application.Json)
            setBody(invalidLike)
        }
        assertEquals(HttpStatusCode.NotFound, doesntExist.status)

        val pinIdIsString = client.delete("/pins/string/likes") {
            contentType(ContentType.Application.Json)
            setBody(validLike)
        }
        assertEquals(HttpStatusCode.BadRequest, pinIdIsString.status)
    }
    @Test
    fun getPinsByUser() = pinTestApp {
        val resOK = client.get("/pins/users/${existingUser.id}")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val idIsString = client.get("/pins/users/string")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun getBookmarksByUser() = pinTestApp {
        val resOK = client.get("/pins/users/${existingUser.id}/bookmarks")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val idIsString = client.get("/pins/users/string/bookmarks")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun createBookmark() = pinTestApp {
        val resOK = client.post("/pins/users/${existingUser.id}/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(newBookmark)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.post("/pins/users/${newUser.id}/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(user2Bookmark)
        }
        assertEquals(HttpStatusCode.InternalServerError, doesntExist.status)

        val pathDoesntMatchDTO = client.post("/pins/users/${existingUser.id}/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(user2Bookmark)
        }
        assertEquals(HttpStatusCode.BadRequest, pathDoesntMatchDTO.status)

        val idIsString = client.post("/pins/users/string/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(newBookmark)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun deleteBookmark() = pinTestApp {
        val resOK = client.delete("/pins/users/${existingUser.id}/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(existingBookmark)
        }
        assertEquals(HttpStatusCode.OK, resOK.status)

        val doesntExist = client.delete("/pins/users/${newUser.id}/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(user2Bookmark)
        }
        assertEquals(HttpStatusCode.NotFound, doesntExist.status)

        val idIsString = client.delete("/pins/users/string/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(existingBookmark)
        }
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
    @Test
    fun getFeed() = pinTestApp {
        val resOK = client.get("/pins/users/${existingUser.id}/feed")
        assertEquals(HttpStatusCode.OK, resOK.status)

        val idIsString = client.get("/pins/users/string/feed")
        assertEquals(HttpStatusCode.BadRequest, idIsString.status)
    }
}