package com.example.usecases

import com.example.database.IDatabase
import net.codebot.models.CommentDTO
import net.codebot.models.CreateCommentDTO

class CommentService (db: IDatabase): ICommentService {
    private val db: IDatabase = db

    override suspend fun createComment(dto: CreateCommentDTO): CommentDTO? {
        return db.transaction {
            val id = db.createComment(dto)
            if (!dto.photos.isNullOrEmpty()) {
                for (photo in dto.photos!!) {
                    db.createPhoto(photo, id, dto.pinId)
                }
            }
            return@transaction db.getCommentDTO(id)
        }
    }

    override suspend fun deleteComment(commentId: Int): CommentDTO? {
        return db.transaction {
            val ret = db.getCommentDTO(commentId)
            db.deleteComment(commentId)
            db.deletePhotosByComment(commentId)
            return@transaction ret
        }
    }
}