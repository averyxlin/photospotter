package com.example.usecases

import net.codebot.models.CommentDTO
import net.codebot.models.CreateCommentDTO

class MockCommentService: ICommentService {
    override suspend fun createComment(dto: CreateCommentDTO): CommentDTO? {
        return if (dto == validCreateCommentDTO) {
            existingComment
        } else {
            null
        }
    }

    override suspend fun deleteComment(commentId: Int): CommentDTO? {
        return if (commentId == existingComment.id) {
            existingComment
        } else {
            null
        }
    }
}