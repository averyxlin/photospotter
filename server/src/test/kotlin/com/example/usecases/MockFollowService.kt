package com.example.usecases

import net.codebot.models.UpdateFollowingDTO
import net.codebot.models.UserDTO

class MockFollowService: IFollowService {
    override suspend fun createFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO? {
        return if (dto == newUpdateFollowingDTO) {
            newUpdateFollowingDTO
        } else {
            null
        }
    }

    override suspend fun deleteFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO? {
        return if (dto == existingUpdateFollowingDTO) {
            existingUpdateFollowingDTO
        } else {
            null
        }
    }

    override suspend fun getFollowers(userId: Int): List<UserDTO> {
        return listOf()
    }

    override suspend fun getFollowing(userId: Int): List<UserDTO> {
        return listOf()
    }
}