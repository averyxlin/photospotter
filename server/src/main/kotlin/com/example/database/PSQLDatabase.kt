package com.example.database

import kotlinx.coroutines.Dispatchers
import net.codebot.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

class PSQLDatabase: IDatabase {
    private val db = Database.connect("jdbc:postgresql://ep-divine-silence-a5qxkdfj.us-east-2.aws.neon.tech/neondb",
        driver = "org.postgresql.Driver",
        user = "williambrandontran", password = "YFPNfoZkR8g7")

    init {
        transaction(db) {
            SchemaUtils.create(
                Pins,
                PinTags,
                Tags,
                Comments,
                Photos,
                Users,
                UsersFollowing,
                UserPinLikes,
                UserBookmarkedPins,
                )
        }
    }

    private val userRepository = UserRepository()
    private val followRepository = FollowRepository()
    private val pinRepository = PinRepository()
    private val bookmarkRepository = BookmarkRepository()
    private val commentRepository = CommentRepository()
    private val tagRepository = TagRepository()
    private val likeRepository = LikeRepository()
    private val photoRepository = PhotoRepository()

    override suspend fun <T> transaction(block: suspend () -> T): T {
        return dbQuery(block)
    }

    // USER REPOSITORY
    override suspend fun getUser(userId: Int) : UserDTO? {
        return userRepository.getUser(userId)
    }
    override suspend fun getUserByAuthId(authId: String) : UserDTO? {
        return userRepository.getUserByAuthId(authId)
    }
    override suspend fun createUser(user: CreateUserDTO) : Int {
        return userRepository.createUser(user)
    }
    override suspend fun updateUser(id: Int, user: UserDTO) {
        return userRepository.updateUser(id, user)
    }
    override suspend fun deleteUser(userId: Int) {
        return userRepository.deleteUser(userId)
    }
    // FOLLOW REPOSITORY
    override suspend fun getFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO? {
        return followRepository.getFollow(dto)
    }
    override suspend fun createFollow(dto: UpdateFollowingDTO) {
        return followRepository.createFollow(dto)
    }
    override suspend fun deleteFollow(dto: UpdateFollowingDTO) {
        return followRepository.deleteFollow(dto)
    }
    override suspend fun getFollowers(userId: Int): List<UserDTO> {
        return followRepository.getFollowers(userId)
    }
    override suspend fun getFollowing(userId: Int): List<UserDTO> {
        return followRepository.getFollowing(userId)
    }
    // PIN REPOSITORY
    override suspend fun getAllPins(tagId: Int?) : List<PinDTO> {
        return pinRepository.getAllPins(tagId)
    }
    override suspend fun getPin(pinId: Int) : PinDTO? {
        return pinRepository.getPin(pinId)
    }
    override suspend fun getPinsByUserId(userId: Int) : List<PinDTO> {
        return pinRepository.getPinsByUserId(userId)
    }
    override suspend fun getPinsFromFollowing(userId: Int) : List<PinDTO> {
        return pinRepository.getPinsFromFollowing(userId)
    }
    override suspend fun createPin(dto: CreatePinDTO) : Int {
        return pinRepository.createPin(dto)
    }
    override suspend fun updatePin(id: Int, pin: PinDTO) {
        return pinRepository.updatePin(id, pin)
    }
    override suspend fun deletePin(pinId: Int) {
        return pinRepository.deletePin(pinId)
    }
    // BOOKMARK REPOSITORY
    override suspend fun getBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO? {
        return bookmarkRepository.getBookmark(dto)
    }
    override suspend fun createBookmark(dto: UpdateBookmarkDTO) {
        return bookmarkRepository.createBookmark(dto)
    }
    override suspend fun getBookmarks(userId: Int) : List<PinDTO> {
        return bookmarkRepository.getBookmarks(userId)
    }
    override suspend fun deleteBookmark(dto: UpdateBookmarkDTO) {
        return bookmarkRepository.deleteBookmark(dto)
    }
    // COMMENT REPOSITORY
    override suspend fun getPinComments(pinId: Int) : List<CommentDTO> {
        return commentRepository.getPinComments(pinId)
    }
    override suspend fun getCommentDTO(id: Int) : CommentDTO? {
        return commentRepository.getCommentDTO(id)
    }
    override suspend fun createComment(dto: CreateCommentDTO): Int {
        return commentRepository.createComment(dto)
    }
    override suspend fun deleteComment(commentId: Int) {
        return commentRepository.deleteComment(commentId)
    }
    // TAG REPOSITORY
    override suspend fun getAllTagOptions() : List<TagDTO> {
        return tagRepository.getAllTagOptions()
    }
    override suspend fun createPinTag(pinId: Int, tagId: Int) {
        return tagRepository.createPinTag(pinId, tagId)
    }
    override suspend fun getTagIdByName(name: String) : Int? {
        return tagRepository.getTagIdByName(name)
    }
    override suspend fun createTag(name: String): Int  {
        return tagRepository.createTag(name)
    }
    override suspend fun getPinTag(pinId: Int, tagId: Int) : TagDTO? {
        return tagRepository.getPinTag(pinId, tagId)
    }
    override suspend fun deletePinTag(pinId: Int, tagId: Int) {
        return tagRepository.deletePinTag(pinId, tagId)
    }
    override suspend fun deleteTag(tagId: Int) {
        return tagRepository.deleteTag(tagId)
    }
    override suspend fun getPinTags(pinId: Int) : List<TagDTO>{
        return tagRepository.getPinTags(pinId)
    }
    // LIKE REPOSITORY
    override suspend fun getLike(dto: LikeDTO) : LikeDTO? {
        return likeRepository.getLike(dto)
    }
    override suspend fun createLike(dto: LikeDTO) {
        return likeRepository.createLike(dto)
    }

    override suspend fun getLikeUserIds(pinId: Int) : List<Int> {
        return likeRepository.getLikeUserIds(pinId)
    }
    override suspend fun deleteLike(dto: LikeDTO) {
        return likeRepository.deleteLike(dto)
    }
    override suspend fun getLikesCount(pinId: Int): Int {
        return likeRepository.getLikesCount(pinId)
    }
    // PHOTO REPOSITORY
    override suspend fun createPhoto(url: String, commentId: Int?, pinId: Int) {
        return photoRepository.createPhoto(url, commentId, pinId)
    }
    override suspend fun getPhotoURLsByPin(pinId: Int): List<String> {
        return photoRepository.getPhotoURLsByPin(pinId)
    }
    override suspend fun deletePhotosByPin(pinId: Int) {
        return photoRepository.deletePhotosByPin(pinId)
    }
    override suspend fun deletePhotosByComment(commentId: Int) {
        return photoRepository.deletePhotosByComment(commentId)
    }
}