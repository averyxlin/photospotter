package com.example.controllers

import com.example.usecases.IFollowService
import com.example.usecases.IUserService
import com.example.services.IAuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.codebot.models.CreateUserDTO
import net.codebot.models.UpdateFollowingDTO
import net.codebot.models.UserDTO

fun Route.userRoutes(userService: IUserService,
                     followService: IFollowService,
                     authService: IAuthService
) {
    route("/users") {
        post {
            val userDTO = call.receive<CreateUserDTO>()

            val ret = userService.createUser(userDTO)
            if (ret == null) {
                call.response.status(HttpStatusCode.InternalServerError)
                call.respond("Error creating user")
                return@post
            }
            call.respond(ret)
        }
        route("/{userId}") {
            get {
                val userId = tryParsePathParam(call, "userId") ?: return@get
                val ret = userService.getUser(userId)
                if (ret == null) {
                    call.response.status(HttpStatusCode.NotFound)
                    call.respond("User not found")
                    return@get
                }
                call.respond(ret)
            }
            put {
                val userId = tryParsePathParam(call, "userId") ?: return@put
                val updatedUser = call.receive<UserDTO>()

                if (userId != updatedUser.id) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respond("Path id does not match user id")
                    return@put
                }

                val token = call.request.headers["Authorization"]
                if (!authService.verifyUserToken(userId, token)) {
                    call.response.status(HttpStatusCode.Unauthorized)
                    call.respond("User id is not authorized")
                    return@put
                }

                val ret = userService.updateUser(userId, updatedUser)
                if (ret == null) {
                    call.response.status(HttpStatusCode.InternalServerError)
                    call.respond("Error updating user")
                    return@put
                }
                call.respond(ret)
            }
            delete {
                var userId = tryParsePathParam(call, "userId") ?: return@delete

                val token = call.request.headers["Authorization"]
                if (!authService.verifyUserToken(userId, token)) {
                    call.response.status(HttpStatusCode.Unauthorized)
                    call.respond("User id is not authorized")
                    return@delete
                }

                val ret = userService.deleteUser(userId)
                if (ret == null) {
                    call.response.status(HttpStatusCode.InternalServerError)
                    call.respond("Error deleting user")
                    return@delete
                }
                call.respond(ret)
            }
            route("/following") {
                get {
                    val userId = tryParsePathParam(call, "userId") ?: return@get
                    val ret = followService.getFollowing(userId)
                    call.respond(ret)
                }
                post {
                    val userId = tryParsePathParam(call, "userId") ?: return@post
                    val updateFollowingDTO = call.receive<UpdateFollowingDTO>()
                    if (userId != updateFollowingDTO.followerId) {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("Path id does not match follower user id")
                        return@post
                    }

                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@post
                    }

                    val ret = followService.createFollow(updateFollowingDTO)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.InternalServerError)
                        call.respond("Error creating follow")
                        return@post
                    }
                    call.respond(ret)
                }
                delete {
                    val userId = tryParsePathParam(call, "userId") ?: return@delete
                    val updateFollowingDTO = call.receive<UpdateFollowingDTO>()
                    if (userId != updateFollowingDTO.followeeId) {
                        call.response.status(HttpStatusCode.BadRequest)
                        call.respond("Path id does not match user id")
                        return@delete
                    }

                    val token = call.request.headers["Authorization"]
                    if (!authService.verifyUserToken(userId, token)) {
                        call.response.status(HttpStatusCode.Unauthorized)
                        call.respond("User id is not authorized")
                        return@delete
                    }

                    val ret = followService.deleteFollow(updateFollowingDTO)
                    if (ret == null) {
                        call.response.status(HttpStatusCode.NotFound)
                        call.respond("Follow not found")
                        return@delete
                    }
                    call.respond(ret)
                }
            }
            route("/followers") {
                get {
                    val userId = tryParsePathParam(call, "userId") ?: return@get
                    val ret = followService.getFollowers(userId)
                    call.respond(ret)
                }
            }
        }
    }
}