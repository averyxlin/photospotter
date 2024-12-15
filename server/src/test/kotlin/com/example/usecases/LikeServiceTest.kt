package com.example.usecases

import com.example.database.MockDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LikeServiceTest {
    private val likeService = LikeService(MockDatabase())

    @Test
    fun createLike() = runBlocking{
        assertEquals(validLike, likeService.createLike(validLike))
        assertEquals(null, likeService.createLike(invalidLike))
    }
    @Test
    fun deleteLike() = runBlocking{
        assertEquals(validLike, likeService.deleteLike(validLike))
        assertEquals(null, likeService.deleteLike(invalidLike))
    }
}