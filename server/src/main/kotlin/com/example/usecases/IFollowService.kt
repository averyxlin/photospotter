package com.example.usecases

import net.codebot.models.UpdateFollowingDTO
import net.codebot.models.UserDTO

interface IFollowService {
    suspend fun createFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO?

    suspend fun deleteFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO?

    suspend fun getFollowers(userId: Int): List<UserDTO>

    suspend fun getFollowing(userId: Int): List<UserDTO>
}