package com.example.usecases

import com.example.database.IDatabase
import net.codebot.models.LikeDTO

class LikeService (db: IDatabase): ILikeService {
    private val db: IDatabase = db

    override suspend fun createLike(dto: LikeDTO) : LikeDTO? {
        db.createLike(dto)
        return db.getLike(dto)
    }

    override suspend fun deleteLike(dto: LikeDTO): LikeDTO?  {
        val ret = db.getLike(dto) ?: return null
        db.deleteLike(dto)
        return ret
    }
}