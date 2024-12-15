package com.example.controllers

import com.example.usecases.IUserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(userService: IUserService) {
    route("/auth") {
        get ("/login/{authId}") {
            val authId = call.parameters["authId"]!!
            val ret = userService.getUserByAuthId(authId)
            if (ret == null) {
                call.response.status(HttpStatusCode.NotFound)
                call.respond("User not found")
                return@get
            }
            call.respond(ret)
        }
    }
}