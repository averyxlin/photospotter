package com.example.usecases

import com.example.database.IDatabase
import net.codebot.models.PinDTO
import net.codebot.models.UpdateBookmarkDTO

class BookmarkService (db: IDatabase): IBookmarkService {
    private val db: IDatabase = db

    override suspend fun createBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO? {
        db.createBookmark(dto)
        return db.getBookmark(dto)
    }

    override suspend fun getBookmarks(userId: Int) : List<PinDTO> {
        return db.transaction {
            val pins = db.getBookmarks(userId)
            val ret = mutableListOf<PinDTO>()
            for (pin in pins) {
                val user = db.getUser(pin.userId)
                val tags = db.getPinTags(pin.id)
                val photos = db.getPhotoURLsByPin(pin.id)
                val likes = db.getLikesCount(pin.id)
                val comments = db.getPinComments(pin.id)
                val usersWhoLiked = db.getLikeUserIds(pin.id)
                ret.add(
                    pin.copy(
                        user = user,
                        tags = tags,
                        photos = photos,
                        likes = likes,
                        comments = comments,
                        likedByUserIds = usersWhoLiked
                    )
                )
            }
            return@transaction ret
        }
    }

    override suspend fun deleteBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO? {
        val ret = db.getBookmark(dto) ?: return null
        db.deleteBookmark(dto)
        return ret
    }
}