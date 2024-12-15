package com.example.services

import com.example.database.IDatabase

interface IAuthService {
    suspend fun verifyUserToken(userId: Int, token: String?): Boolean
}