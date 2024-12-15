package com.example.usecases

import com.example.database.IDatabase
import net.codebot.models.UpdateFollowingDTO
import net.codebot.models.UserDTO

class FollowService (db: IDatabase): IFollowService {
    private val db: IDatabase = db

    override suspend fun createFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO? {
        db.createFollow(dto)
        return db.getFollow(dto)
    }

    override suspend fun deleteFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO? {
        val ret = db.getFollow(dto) ?: return null
        db.deleteFollow(dto)
        return ret
    }

    override suspend fun getFollowers(userId: Int): List<UserDTO> {
        return db.getFollowers(userId)
    }

    override suspend fun getFollowing(userId: Int): List<UserDTO> {
        return db.getFollowing(userId)
    }
}