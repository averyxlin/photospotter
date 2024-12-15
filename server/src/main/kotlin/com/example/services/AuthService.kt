package com.example.services
import com.example.database.IDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class AuthService (db: IDatabase): IAuthService {
    private val db: IDatabase = db
    private val app: FirebaseApp? = FirebaseApp.initializeApp()
    override suspend fun verifyUserToken(userId: Int, token: String?): Boolean {
        if (token == null) {
            return false
        }
        val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
        val uid = decodedToken.uid
        val user = db.getUserByAuthId(uid)
        return user != null && user.id == userId
    }
}