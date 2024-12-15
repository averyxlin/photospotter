package com.example.database

import net.codebot.models.LikeDTO
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class LikeRepository {
    suspend fun getLike(dto: LikeDTO) : LikeDTO? {
        return dbQuery {
            UserPinLikes.select { (UserPinLikes.pinId eq dto.pinId) and
                    (UserPinLikes.userId eq dto.userId)}
                .map { LikeDTO(
                    it[UserPinLikes.pinId],
                    it[UserPinLikes.userId],
                ) }.single()
        }
    }

    suspend fun getLikeUserIds(pinId: Int) : List<Int> {
        return dbQuery {
            UserPinLikes
                    .select(UserPinLikes.pinId eq pinId)
                    .map { it[UserPinLikes.userId]}
        }
    }
    suspend fun createLike(dto: LikeDTO) {
        return dbQuery {
            UserPinLikes.insert {
                it[pinId] = dto.pinId
                it[userId] = dto.userId
            }
        }
    }

    suspend fun deleteLike(dto: LikeDTO) {
        dbQuery {
            UserPinLikes.deleteWhere {
                (pinId eq dto.pinId) and
                        (userId eq dto.userId)
            }
        }
    }
    suspend fun getLikesCount(pinId: Int): Int {
        return dbQuery {
            UserPinLikes
                .select(UserPinLikes.pinId eq pinId)
                .count()
                .toInt()
        }
    }
}