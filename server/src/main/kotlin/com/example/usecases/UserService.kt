package com.example.usecases

import com.example.database.IDatabase
import net.codebot.models.CreateUserDTO
import net.codebot.models.UserDTO

class UserService (db: IDatabase): IUserService {
    private val db: IDatabase = db
    override suspend fun getUser(userId: Int): UserDTO? {
        val user = db.getUser(userId) ?: return null
        val followerCount = db.getFollowers(userId).count()
        val followingCount = db.getFollowing(userId).count()
        return user.copy(
            followerCount=followerCount,
            followingCount=followingCount
        )
    }

    override suspend fun getUserByAuthId(authId: String): UserDTO? {
        return db.getUserByAuthId(authId);
    }

    override suspend fun createUser(user: CreateUserDTO): UserDTO? {
        val userId = db.createUser(user);
        return db.getUser(userId)
    }

    override suspend fun updateUser(userId: Int, dto: UserDTO): UserDTO? {
        db.updateUser(userId, dto);
        return db.getUser(userId)
    }

    override suspend fun deleteUser(userId: Int): UserDTO? {
        var ret = db.getUser(userId) ?: return null;
        db.deleteUser(userId);
        return ret
    }
}