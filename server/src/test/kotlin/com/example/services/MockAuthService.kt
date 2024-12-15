package com.example.services

import com.google.firebase.auth.FirebaseAuth

class MockAuthService: IAuthService {
    override suspend fun verifyUserToken(userId: Int, token: String?): Boolean {
        return true
    }
}