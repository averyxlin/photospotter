package com.example.usecases

import com.example.database.IDatabase
import com.example.services.AuthService

class Services (db: IDatabase) {
    private val db: IDatabase = db
    val authService = AuthService(db)
    val bookmarkService = BookmarkService(db)
    val commentService = CommentService(db)
    val followService = FollowService(db)
    val likeService = LikeService(db)
    val pinService = PinService(db)
    val tagService = TagService(db)
    val userService = UserService(db)
}