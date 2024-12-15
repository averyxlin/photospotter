package com.example.database

import net.codebot.models.CommentDTO
import net.codebot.models.CreateCommentDTO
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime

class CommentRepository {
    suspend fun getPinComments(pinId: Int) : List<CommentDTO> {
        return dbQuery {
            Comments.select{ Comments.pinId eq pinId }.map {
                CommentDTO(
                    it[Comments.id],
                    it[Comments.pinId],
                    it[Comments.content],
                    it[Comments.userId],
                    Photos.select{Photos.commentId eq it[Comments.id]}.map{photoIt -> photoIt[Photos.photoURL]},
                    it[Comments.createdAt],
                ) }
        }
    }
    suspend fun getCommentDTO(id: Int) : CommentDTO? {
        return try {
            dbQuery {
                Comments.select{ Comments.id eq id }.map {
                    CommentDTO(
                        it[Comments.id],
                        it[Comments.pinId],
                        it[Comments.content],
                        it[Comments.userId],
                        Photos.select{Photos.commentId eq id}.map{photoIt -> photoIt[Photos.photoURL]},
                        it[Comments.createdAt],
                    ) }.single()
            }
        } catch (e: NoSuchElementException) {
            null
        }
    }
    suspend fun createComment(dto: CreateCommentDTO): Int {
        return dbQuery {
            val currentTimestamp = LocalDateTime.now()
            val newId =
                Comments.insert {
                    it[content] = dto.content
                    it[userId] = dto.userId
                    it[pinId] = dto.pinId
                    it[createdAt] = currentTimestamp.toString()
                }[Comments.id]
            return@dbQuery newId
        }

    }

    suspend fun deleteComment(commentId: Int) {
        dbQuery {
            Comments.deleteWhere {
                (id eq commentId)
            }
        }
    }
}