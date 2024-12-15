package com.example.usecases

import com.example.database.MockDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentServiceTest {
    private val commentService = CommentService(MockDatabase())

    @Test
    fun createComment() = runBlocking{
        assertEquals(existingComment, commentService.createComment(validCreateCommentDTO))
        assertEquals(commentWithPhotos, commentService.createComment(createCommentWithPhotos))
    }

    @Test
    fun deleteBookmark() = runBlocking{
        assertEquals(existingComment, commentService.deleteComment(existingComment.id))
        assertEquals(null, commentService.deleteComment(newComment.id))
    }
}