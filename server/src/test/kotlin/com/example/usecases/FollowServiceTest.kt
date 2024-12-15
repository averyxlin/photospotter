package com.example.usecases

import com.example.database.MockDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FollowServiceTest {
    private val followService = FollowService(MockDatabase())

    @Test
    fun createFollow() = runBlocking{
        assertEquals(validUpdateFollowingDTO, followService.createFollow(validUpdateFollowingDTO))
        assertEquals(null, followService.createFollow(invalidUpdateFollowingDTO))
    }
    @Test
    fun deleteFollow() = runBlocking{
        assertEquals(validUpdateFollowingDTO, followService.deleteFollow(validUpdateFollowingDTO))
        assertEquals(null, followService.deleteFollow(invalidUpdateFollowingDTO))
    }
    @Test
    fun getFollowers() = runBlocking{
        assertEquals(listOf(popularUser), followService.getFollowers(existingUser.id))
        assertEquals(listOf(), followService.getFollowers(newUser.id))
    }
    @Test
    fun getFollowing() = runBlocking{
        assertEquals(listOf(popularUser), followService.getFollowers(existingUser.id))
        assertEquals(listOf(), followService.getFollowers(newUser.id))
    }
}