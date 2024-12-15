package com.example.usecases

import com.example.database.IDatabase
import net.codebot.models.CreatePinDTO
import net.codebot.models.PinDTO

class PinService (db: IDatabase): IPinService {
    private val db: IDatabase = db
    private val tagService = TagService(db)
    override suspend fun getAllPins(tagId: Int?) : List<PinDTO> {
        return db.getAllPins(tagId)
    }

    override suspend fun getPin(pinId: Int) : PinDTO? {
        return db.transaction {
            val pin = db.getPin(pinId) ?: return@transaction null
            val user = db.getUser(pin.userId)
            val tags = db.getPinTags(pin.id)
            val photos = db.getPhotoURLsByPin(pin.id)
            val likes = db.getLikesCount(pin.id)
            val comments = db.getPinComments(pin.id)
            val usersWhoLiked = db.getLikeUserIds(pin.id)
            return@transaction pin.copy(
                user = user,
                tags = tags,
                photos = photos,
                likes = likes,
                comments = comments,
                likedByUserIds = usersWhoLiked
            )
        }
    }

    override suspend fun getPinsByUserId(userId: Int) : List<PinDTO> {
        return db.transaction {
            val pins = db.getPinsByUserId(userId)
            val ret = mutableListOf<PinDTO>()
            for (pin in pins) {
                val user = db.getUser(pin.userId)
                val tags = db.getPinTags(pin.id)
                val photos = db.getPhotoURLsByPin(pin.id)
                val likes = db.getLikesCount(pin.id)
                val comments = db.getPinComments(pin.id)
                val usersWhoLiked = db.getLikeUserIds(pin.id)
                ret.add(pin.copy(
                    user = user,
                    tags = tags,
                    photos = photos,
                    likes = likes,
                    comments = comments,
                    likedByUserIds = usersWhoLiked
                ))
            }
            return@transaction ret
        }
    }

    override suspend fun getPinsFromFollowing(userId: Int) : List<PinDTO> {
        return db.transaction {
            val pins = db.getPinsFromFollowing(userId)
            val ret = mutableListOf<PinDTO>()
            for (pin in pins) {
                val user = db.getUser(pin.userId)
                val tags = db.getPinTags(pin.id)
                val photos = db.getPhotoURLsByPin(pin.id)
                val likes = db.getLikesCount(pin.id)
                val comments = db.getPinComments(pin.id)
                val usersWhoLiked = db.getLikeUserIds(pin.id)
                ret.add(pin.copy(
                    user = user,
                    tags = tags,
                    photos = photos,
                    likes = likes,
                    comments = comments,
                    likedByUserIds = usersWhoLiked
                ))
            }
            return@transaction ret
        }
    }

    override suspend fun createPin(dto: CreatePinDTO) : PinDTO? {
        return db.transaction {
            val newId = db.createPin(dto)
            if (!dto.photos.isNullOrEmpty()) {
                for (photo in dto.photos!!) {
                    db.createPhoto(photo, null, newId)
                }
            }
            if (!dto.tags.isNullOrEmpty()) {
                for (tag in dto.tags!!) {
                    tagService.createPinTag(tag.copy(pinId = newId))
                }
            }
            return@transaction getPin(newId)
        }
    }

    override suspend fun updatePin(id: Int, pin: PinDTO) : PinDTO? {
        db.updatePin(id, pin)
        return db.getPin(id)
    }

    override suspend fun deletePin(pinId: Int): PinDTO? {
        return db.transaction {
            val ret = db.getPin(pinId) ?: return@transaction null
            db.deletePin(pinId)
            if (ret.tags !== null) {
                for (t in ret.tags!!) {
                    if (db.getAllPins(t.tagId).count() == 1) {
                        db.deleteTag(t.tagId)
                    }
                }
            }
            if (ret.photos !== null) {
                db.deletePhotosByPin(pinId)
            }

            return@transaction ret
        }
    }
}