package com.example.usecases

import net.codebot.models.CreateUserDTO
import net.codebot.models.UserDTO

class MockUserService : IUserService {
    override suspend fun getUser(userId: Int): UserDTO? {
        return if (userId == existingUser.id) {
            existingUser
        } else {
            null
        }
    }
    override suspend fun getUserByAuthId(authId: String): UserDTO? {
        return if (authId == existingUser.authId) {
            existingUser
        } else {
            null
        }
    }
    override suspend fun createUser(user: CreateUserDTO): UserDTO? {
        return if (user == validCreateUserDTO) {
            existingUser
        } else {
            null
        }
    }
    override suspend fun updateUser(userId: Int, dto: UserDTO): UserDTO? {
        return if (dto == newUser) {
            null
        } else {
            dto
        }
    }
    override suspend fun deleteUser(userId: Int): UserDTO? {
        return if (userId == newUser.id) {
            null
        } else {
            existingUser
        }
    }
}