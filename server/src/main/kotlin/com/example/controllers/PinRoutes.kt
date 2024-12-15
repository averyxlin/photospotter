package com.example.controllers

import com.example.usecases.*
import io.ktor.http.*
import io.ktor.server.request.*
import net.codebot.models.*
import com.example.services.IAuthService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.pinRoutes(bookmarkService: IBookmarkService,
                    commentService: ICommentService,
                    likeService: ILikeService,
                    pinService: IPinService,
                    tagService: ITagService,
                    authService: IAuthService
) {
    route("/pins") {
        get {
            val tagStr = call.request.queryParameters["tag"]
            if (!tagStr.isNullOrEmpty()) {
                val tag = tagStr.toInt()
                val ret = pinService.getAllPins(tag)
                call.respond(ret)
                return@get
            }
            val ret = pinService.getAllPins(null)
            call.respond(ret)
        }
        post {
            val dto = call.receive<CreatePinDTO>()
            val token = call.request.headers["Authorization"]
            if (!authService.verifyUserToken(dto.userId, token)) {
                call.response.status(HttpStatusCode.Unauthorized)
                call.respond("User id is not authorized")
                return@post
            }

            val ret = pinService.createPin(dto)
            if (ret == null) {
                call.response.status(HttpStatusCode.InternalServerError)
                call.respond("Error creating pin")
                return@post
            }
            call.respond(ret)
        }
        get ("/tags/options") {
            val ret = tagService.getAllTagOptions()
            call.respond(ret)
        }
        route("/{pinId}") {
            get {
                val pinId = tryParsePathParam(call, "pinId") ?: return@get
                val ret = pinService.getPin(pinId)
                if (ret == null) {
                    call.response.status(HttpStatusCode.NotFound)
                    call.respond("Pin not found")
                    return@get
                }
                call.respond(ret)
            }
            put {
                val pinId = tryParsePathParam(call, "pinId") ?: return@put
                val pinDTO = call.receive<PinDTO>()

                if (pinId != pinDTO.id) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respond("Path Id does not match pin Id")
                    return@put
                }

                val token = call.request.headers["Authorization"]
                if (!authService.verifyUserToken(pinDTO.userId, token)) {
                    call.response.status(HttpStatusCode.Unauthorized)
                    call.respond("User id is not authorized")
                    return@put
                }

                val ret = pinService.updatePin(pinId, pinDTO)
                if (ret == null) {
                    call.response.status(HttpStatusCode.InternalServerError)
                    call.respond("Error updating pin")
                    return@put
                }
                call.respond(ret)
            }
            delete {
                val pinId = tryParsePathParam(call, "pinId") ?: return@delete

                val pin = pinService.getPin(pinId)
                if (pin == null) {
                    call.response.status(HttpStatusCode.NotFound)
                    call.respond("Pin not found")
                    return@delete
                }

                val token = call.request.headers["Authorization"]
                if (!authService.verifyUserToken(pin.userId, token)) {
                    call.response.status(HttpStatusCode.Unauthorized)
                    call.respond("User id is not authorized")
                    return@delete
                }

                val ret = pinService.deletePin(pinId)
                if (ret == null) {
                    call.response.status(HttpStatusCode.NotFound)
                    call.respond("Pin not found")
                    return@delete
                }
                call.respond(ret)
            }
            route("/comments") {
                post {
                    val pinId = tryParsePathParam(call, "pinId") ?: return@post
                    val commentDTO = call.receive<CreateCommentDTO>()
                    if (pinId != commentDTO.pinId) {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("Path id does not match pin id")
                        return@post
                    }

                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(commentDTO.userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@post
                    }

                    val ret = commentService.createComment(commentDTO)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.InternalServerError)
                        call.respond("Error creating comment")
                        return@post
                    }
                    call.respond(ret)
                }
                delete ("/{commentId}"){
                    val commentId = tryParsePathParam(call, "commentId") ?: return@delete

                    val ret = commentService.deleteComment(commentId)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.NotFound)
                        call.respond("Comment not found")
                        return@delete
                    }
                    call.respond(ret)
                }
            }

            route("/tags") {
                post {
                    val pinId = tryParsePathParam(call, "pinId") ?: return@post
                    val dto = call.receive<CreateTagDTO>()
                    if (pinId != dto.pinId) {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("Path id does not match dto id")
                        return@post
                    }

                    val ret = tagService.createPinTag(dto)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.InternalServerError)
                        call.respond("Error creating tag")
                        return@post
                    }
                    call.respond(ret)
                }
                delete ("/{tagId}"){
                    val pinId = tryParsePathParam(call, "pinId") ?: return@delete
                    val tagId = tryParsePathParam(call, "tagId") ?: return@delete
                    val ret = tagService.deletePinTag(pinId, tagId)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.NotFound)
                        call.respond("Tag not found")
                        return@delete
                    }
                    call.respond(ret)
                }
            }
            route("/likes") {
                post {
                    val pinId = tryParsePathParam(call, "pinId") ?: return@post
                    val dto = call.receive<LikeDTO>()
                    if (pinId != dto.pinId) {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("Path id does not match dto id")
                        return@post
                    }

                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(dto.userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@post
                    }

                    val ret = likeService.createLike(dto)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.InternalServerError)
                        call.respond("Error creating like")
                        return@post
                    }
                    call.respond(ret)
                }
                delete {
                    val pinId = tryParsePathParam(call, "pinId") ?: return@delete
                    val dto = call.receive<LikeDTO>()
                    if (pinId != dto.pinId) {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("Path id does not match dto id")
                        return@delete
                    }

                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(dto.userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@delete
                    }

                    val ret = likeService.deleteLike(dto)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.NotFound)
                        call.respond("Like not found")
                        return@delete
                    }
                    call.respond(ret)
                }
            }
        }
        route("/users/{userId}") {
            get {
                val userId = tryParsePathParam(call, "userId") ?: return@get
                val ret = pinService.getPinsByUserId(userId)
                call.respond(ret)
            }
            route("/bookmarks") {
                get {
                    val userId = tryParsePathParam(call, "userId") ?: return@get

                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@get
                    }

                    val ret = bookmarkService.getBookmarks(userId)
                    call.respond(ret)
                }
                post {
                    val userId = tryParsePathParam(call, "userId") ?: return@post
                    val dto = call.receive<UpdateBookmarkDTO>()
                    if (userId != dto.userId) {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("Path id does not match dto id")
                        return@post
                    }

                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@post
                    }

                    val ret = bookmarkService.createBookmark(dto)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.InternalServerError)
                        call.respond("Error creating bookmark")
                        return@post
                    }
                    call.respond(ret)
                }
                delete {
                    val userId = tryParsePathParam(call, "userId") ?: return@delete
                    val dto = call.receive<UpdateBookmarkDTO>()
                    if (userId != dto.userId) {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("Path id does not match dto id")
                        return@delete
                    }

                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@delete
                    }

                    val ret = bookmarkService.deleteBookmark(dto)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.NotFound)
                        call.respond("Bookmark not found")
                        return@delete
                    }
                    call.respond(ret)
                }
            }
            route("/feed") {
                get {
                    val userId = tryParsePathParam(call, "userId") ?: return@get
                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@get
                    }
                    val ret = pinService.getPinsFromFollowing(userId)
                    call.respond(ret)
                }
            }
        }
    }
}