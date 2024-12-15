package com.example.usecases

import net.codebot.models.CreateUserDTO
import net.codebot.models.UserDTO

interface IUserService {
    suspend fun getUser(userId: Int): UserDTO?
    suspend fun getUserByAuthId(authId: String): UserDTO?
    suspend fun createUser(user: CreateUserDTO): UserDTO?
    suspend fun updateUser(userId: Int, dto: UserDTO): UserDTO?
    suspend fun deleteUser(userId: Int): UserDTO?
}