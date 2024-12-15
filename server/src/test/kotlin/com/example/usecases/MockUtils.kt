package com.example.usecases

import net.codebot.models.*

val validCreateUserDTO = CreateUserDTO("myName", "myEmail", "myAuthId")
val invalidCreateUserDTO = CreateUserDTO("bad", "bad", "bad")

val existingUser = UserDTO(1, "myName", "myEmail", "myAuthId")
val newUser = UserDTO(2, "myName", "myEmail", "myAuthId2")
val popularUser = UserDTO(3, "myName", "myEmail", "myAuthId2")

val existingBookmark = UpdateBookmarkDTO(1,1)
val newBookmark = UpdateBookmarkDTO(2,1)
val user2Bookmark = UpdateBookmarkDTO(2,2)
val invalidBookmark = UpdateBookmarkDTO(2,2)

val validCreateCommentDTO = CreateCommentDTO(1, "content", 1, null)
val invalidCreateCommentDTO = CreateCommentDTO(2, "content", 1, null)

val createCommentWithPhotos = CreateCommentDTO(3, "content", 1, listOf("photo"))
val commentWithPhotos = CommentDTO(3,1, "content", 1, listOf("photo"), "myDate")

val existingComment = CommentDTO(1,1, "content", 1, null, "myDate")
val newComment = CommentDTO(2,1, "content", 1, null, "myDate")

val newUpdateFollowingDTO = UpdateFollowingDTO(1,2)
val existingUpdateFollowingDTO = UpdateFollowingDTO(2,1)

val validUpdateFollowingDTO = UpdateFollowingDTO(2,1)
val invalidUpdateFollowingDTO = UpdateFollowingDTO(1,2)

val validLike = LikeDTO(1,1)
val invalidLike = LikeDTO(2,2)

val existingPin = PinDTO(1,"myName", "myAddress", 0.1, 0.1, 1)
val newPin = PinDTO(2,"myName", "myAddress", 0.1, 0.1, 2)

val validCreatePinDTO = CreatePinDTO("myName", "myAddress", 0.1, 0.1, 1)
val invalidCreatePinDTO = CreatePinDTO("myName", "myAddress", 0.1, 0.1, 2)

val validCreateTagDTO = CreateTagDTO("valid", 1)
val invalidCreateTagDTO = CreateTagDTO("invalid", 2)
val createTagNoPin = CreateTagDTO("noPin")

val existingTag = TagDTO(1, "name", 1)
val newTag = TagDTO(2, "name", 2)

val fullPinDTO = existingPin.copy(
    user = existingUser,
    tags = listOf(existingTag),
    photos = listOf("photo"),
    likes = 1,
    comments = listOf(existingComment),
    likedByUserIds = listOf(1)
)

val fullUserDTO = existingUser.copy(
    followingCount = 1,
    followerCount = 1,
)