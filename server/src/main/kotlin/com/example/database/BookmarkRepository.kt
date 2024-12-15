package com.example.database

import net.codebot.models.PinDTO
import net.codebot.models.UpdateBookmarkDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class BookmarkRepository {
    suspend fun getBookmark(dto: UpdateBookmarkDTO) : UpdateBookmarkDTO? {
        return try {
            return dbQuery {
                UserBookmarkedPins.select { (UserBookmarkedPins.pinId eq dto.pinId) and
                        (UserBookmarkedPins.userId eq dto.userId)}
                    .map { UpdateBookmarkDTO(
                        it[UserBookmarkedPins.pinId],
                        it[UserBookmarkedPins.userId],
                    ) }.single()
            }
        } catch (e: NoSuchElementException) {
            null
        }
    }
    suspend fun createBookmark(dto: UpdateBookmarkDTO) {
        dbQuery {
            UserBookmarkedPins.insert {
                it[pinId] = dto.pinId
                it[userId] = dto.userId
            }
        }
    }
    suspend fun getBookmarks(userId: Int) : List<PinDTO> {
        return dbQuery {
            UserBookmarkedPins.join(Pins, JoinType.INNER, UserBookmarkedPins.pinId, Pins.id)
                .select(UserBookmarkedPins.userId eq userId)
                .map {
                    PinDTO(
                        it[Pins.id],
                        it[Pins.name],
                        it[Pins.address],
                        it[Pins.lat],
                        it[Pins.lon],
                        it[Pins.userId],
                        it[Pins.desc],
                        it[Pins.createdAt]
                    )
                }
        }
    }
    suspend fun deleteBookmark(dto: UpdateBookmarkDTO) {
        dbQuery {
            UserBookmarkedPins.deleteWhere {
                (pinId eq dto.pinId) and
                        (userId eq dto.userId)
            }
        }
    }
}