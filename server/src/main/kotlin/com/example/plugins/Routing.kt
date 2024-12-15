package com.example.plugins

import com.example.usecases.Services
import com.example.controllers.authRoutes
import com.example.controllers.pinRoutes
import com.example.controllers.userRoutes
//import com.google.auth.oauth2.GoogleCredentials
//import com.google.firebase.FirebaseApp
//import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
//import java.io.FileInputStream

fun Application.configureRouting(services: Services) {
    // firebase setup
//    val serviceAccount =
//        FileInputStream("path/to/serviceAccountKey.json")
//    val options = FirebaseOptions.Builder()
//        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//        .build()
//    FirebaseApp.initializeApp(options)
    routing {
        get("/") {
            call.respondText("Received")
        }
        authRoutes(services.userService)
        userRoutes(services.userService, services.followService, services.authService)
        pinRoutes(
            services.bookmarkService,
            services.commentService,
            services.likeService,
            services.pinService,
            services.tagService,
            services.authService
        )
    }
}
