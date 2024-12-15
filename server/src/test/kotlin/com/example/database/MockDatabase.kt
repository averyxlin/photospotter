package com.example.database

import com.example.usecases.*
import net.codebot.models.*

class MockDatabase: IDatabase {
    override suspend fun <T> transaction(block: suspend () -> T): T = block()

    // USER REPOSITORY
    override suspend fun getUser(userId: Int) : UserDTO? {
        return if (userId == existingUser.id) {
            existingUser
        } else {
            null
        }
    }
    override suspend fun getUserByAuthId(authId: String) : UserDTO? {
        return if (authId == existingUser.authId) {
            existingUser
        } else {
            null
        }
    }
    override suspend fun createUser(user: CreateUserDTO) : Int {
        return if (user == validCreateUserDTO) {
            existingUser.id
        } else {
            newUser.id
        }
    }
    override suspend fun updateUser(id: Int, user: UserDTO) {
        return
    }
    override suspend fun deleteUser(userId: Int) {
        return
    }
    // FOLLOW REPOSITORY
    override suspend fun getFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO? {
        return if (dto == validUpdateFollowingDTO) {
            validUpdateFollowingDTO
        } else {
            null
        }
    }
    override suspend fun createFollow(dto: UpdateFollowingDTO) {
        return
    }
    override suspend fun deleteFollow(dto: UpdateFollowingDTO) {
        return
    }
    override suspend fun getFollowers(userId: Int): List<UserDTO> {
        return if (userId == existingUser.id) {
            listOf(popularUser)
        } else {
            listOf()
        }
    }
    override suspend fun getFollowing(userId: Int): List<UserDTO> {
        return if (userId == existingUser.id) {
            listOf(popularUser)
        } else {
            listOf()
        }
    }
    // PIN REPOSITORY
    override suspend fun getAllPins(tagId: Int?) : List<PinDTO> {
        return if (tagId == null) {
            listOf(existingPin)
        } else {
            listOf()
        }
    }
    override suspend fun getPin(pinId: Int) : PinDTO? {
        return if (pinId == existingPin.id) {
            existingPin
        } else {
            null
        }
    }
    override suspend fun getPinsByUserId(userId: Int) : List<PinDTO> {
        return if (userId == existingUser.id) {
            listOf(existingPin)
        } else {
            listOf()
        }
    }
    override suspend fun getPinsFromFollowing(userId: Int) : List<PinDTO> {
        return if (userId == existingUser.id) {
            listOf(existingPin)
        } else {
            listOf()
        }
    }
    override suspend fun createPin(dto: CreatePinDTO) : Int {
        return if (dto == validCreatePinDTO) {
            existingPin.id
        } else {
            newPin.id
        }
    }
    override suspend fun updatePin(id: Int, pin: PinDTO) {
        return
    }
    override suspend fun deletePin(pinId: Int) {
        return
    }
    // BOOKMARK REPOSITORY
    override suspend fun getBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO? {
        return if (dto == invalidBookmark) {
            null
        } else {
            dto
        }
    }
    override suspend fun createBookmark(dto: UpdateBookmarkDTO) {
        return
    }
    override suspend fun getBookmarks(userId: Int) : List<PinDTO> {
        return if (userId == existingUser.id) {
            listOf(existingPin)
        } else {
            listOf()
        }
    }
    override suspend fun deleteBookmark(dto: UpdateBookmarkDTO) {
        return
    }
    // COMMENT REPOSITORY
    override suspend fun getPinComments(pinId: Int) : List<CommentDTO> {
        return if (pinId == existingPin.id) {
            listOf(existingComment)
        } else {
            listOf()
        }
    }
    override suspend fun getCommentDTO(id: Int) : CommentDTO? {
        return if (id == existingComment.id) {
            existingComment
        } else if (id == commentWithPhotos.id) {
            commentWithPhotos
        } else {
            null
        }
    }
    override suspend fun createComment(dto: CreateCommentDTO): Int {
        return if (dto == validCreateCommentDTO) {
            existingComment.id
        } else {
            commentWithPhotos.id
        }
    }
    override suspend fun deleteComment(commentId: Int) {
        return
    }
    // TAG REPOSITORY
    override suspend fun getAllTagOptions() : List<TagDTO> {
        return listOf(existingTag)
    }
    override suspend fun createPinTag(pinId: Int, tagId: Int) {
        return
    }
    override suspend fun getTagIdByName(name: String) : Int? {
        return if (name == existingTag.name) {
            existingTag.tagId
        } else {
            null
        }
    }
    override suspend fun createTag(name: String): Int  {
        return existingTag.tagId
    }
    override suspend fun getPinTag(pinId: Int, tagId: Int) : TagDTO? {
        return if (pinId == existingTag.pinId && tagId == existingTag.tagId) {
            existingTag
        } else {
            null
        }
    }
    override suspend fun deletePinTag(pinId: Int, tagId: Int) {
        return
    }
    override suspend fun deleteTag(tagId: Int) {
        return
    }
    override suspend fun getPinTags(pinId: Int) : List<TagDTO>{
        return if (pinId == existingPin.id) {
            listOf(existingTag)
        } else {
            listOf()
        }
    }
    // LIKE REPOSITORY
    override suspend fun getLike(dto: LikeDTO) : LikeDTO? {
        return if (dto == validLike) {
            validLike
        } else {
            null
        }
    }
    override suspend fun createLike(dto: LikeDTO) {
        return
    }

    override suspend fun getLikeUserIds(pinId: Int) : List<Int> {
        return listOf(existingUser.id)
    }
    override suspend fun deleteLike(dto: LikeDTO) {
        return
    }
    override suspend fun getLikesCount(pinId: Int): Int {
        return fullPinDTO.likes!!
    }
    // PHOTO REPOSITORY
    override suspend fun createPhoto(url: String, commentId: Int?, pinId: Int) {
        return
    }
    override suspend fun getPhotoURLsByPin(pinId: Int): List<String> {
        return if (pinId == existingPin.id) {
            fullPinDTO.photos!!
        } else {
            listOf()
        }
    }
    override suspend fun deletePhotosByPin(pinId: Int) {
        return
    }
    override suspend fun deletePhotosByComment(commentId: Int) {
        return
    }
}