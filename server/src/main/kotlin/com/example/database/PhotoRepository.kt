package com.example.database

import net.codebot.models.LikeDTO
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class PhotoRepository {
    suspend fun createPhoto(url: String, commentId: Int?, pinId: Int) {
        return dbQuery {
            Photos.insert {
                it[Photos.photoURL] = url
                it[Photos.commentId] = commentId
                it[Photos.pinId] = pinId
            }
        }
    }

    suspend fun getPhotoURLsByPin(pinId: Int): List<String> {
        return dbQuery {
            Photos.select{Photos.pinId eq pinId}.map{photoIt -> photoIt[Photos.photoURL]}
        }
    }

    suspend fun deletePhotosByPin(pinId: Int) {
        dbQuery {
            Photos.deleteWhere {
                (Photos.pinId eq pinId)
            }
        }
    }

    suspend fun deletePhotosByComment(commentId: Int) {
        dbQuery {
            Photos.deleteWhere {
                (Photos.commentId eq commentId)
            }
        }
    }
}