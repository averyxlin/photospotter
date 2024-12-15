package com.example.usecases

import net.codebot.models.CommentDTO
import net.codebot.models.CreateCommentDTO

interface ICommentService {
    suspend fun createComment(dto: CreateCommentDTO): CommentDTO?
    suspend fun deleteComment(commentId: Int): CommentDTO?
}