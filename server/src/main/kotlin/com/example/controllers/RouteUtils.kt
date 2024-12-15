package com.example.controllers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun tryParsePathParam(call: ApplicationCall, name: String): Int? {
    try {
        val ret = call.parameters[name]?.toInt()
        return ret
    } catch (e: NumberFormatException) {
        call.response.status(HttpStatusCode.BadRequest)
        call.respond("Path id does not match user id")
        return null
    }
}