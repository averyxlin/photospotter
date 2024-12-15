package com.example.database

import net.codebot.models.*
import javax.sql.DataSource

interface IDatabase {
    suspend fun <T> transaction(block: suspend () -> T): T
    //
    // USERS
    //
    suspend fun getUser(userId: Int) : UserDTO?
    suspend fun getUserByAuthId(authId: String) : UserDTO?
    suspend fun createUser(user: CreateUserDTO) : Int
    suspend fun updateUser(id: Int, user: UserDTO)
    suspend fun deleteUser(userId: Int)
    //
    // FOLLOW
    //
    suspend fun getFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO?
    suspend fun createFollow(dto: UpdateFollowingDTO)
    suspend fun getFollowers(userId: Int): List<UserDTO>
    suspend fun getFollowing(userId: Int): List<UserDTO>
    suspend fun deleteFollow(dto: UpdateFollowingDTO)
    //
    // PINS
    //
    suspend fun getAllPins(tagId: Int?) : List<PinDTO>
    suspend fun getPin(pinId: Int) : PinDTO?
    suspend fun getPinsByUserId(userId: Int) : List<PinDTO>
    suspend fun getPinsFromFollowing(userId: Int) : List<PinDTO>
    suspend fun createPin(dto: CreatePinDTO) : Int
    suspend fun updatePin(id: Int, pin: PinDTO)
    suspend fun deletePin(pinId: Int)
    //
    // BOOKMARKS
    //
    suspend fun getBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO?
    suspend fun createBookmark(dto: UpdateBookmarkDTO)
    suspend fun getBookmarks(userId: Int) : List<PinDTO>
    suspend fun deleteBookmark(dto: UpdateBookmarkDTO)
    //
    // COMMENTS
    //
    suspend fun getPinComments(pinId: Int) : List<CommentDTO>
    suspend fun getCommentDTO(id: Int) : CommentDTO?
    suspend fun createComment(dto: CreateCommentDTO): Int
    suspend fun deleteComment(commentId: Int)
    //
    // TAGS
    //
    suspend fun getAllTagOptions() : List<TagDTO>
    suspend fun createPinTag(pinId: Int, tagId: Int)
    suspend fun getTagIdByName(name: String) : Int?
    suspend fun createTag(name: String): Int
    suspend fun getPinTag(pinId: Int, tagId: Int) : TagDTO?
    suspend fun deletePinTag(pinId: Int, tagId: Int)
    suspend fun deleteTag(tagId: Int)
    suspend fun getPinTags(pinId: Int) : List<TagDTO>
    //
    // LIKES
    //
    suspend fun getLike(dto: LikeDTO) : LikeDTO?
    suspend fun getLikeUserIds(pinId: Int) : List<Int>
    suspend fun createLike(dto: LikeDTO)
    suspend fun deleteLike(dto: LikeDTO)
    suspend fun getLikesCount(pinId: Int): Int
    //
    // PHOTOS
    //
    suspend fun createPhoto(url: String, commentId: Int?, pinId: Int)
    suspend fun getPhotoURLsByPin(pinId: Int): List<String>
    suspend fun deletePhotosByPin(pinId: Int)
    suspend fun deletePhotosByComment(commentId: Int)

}