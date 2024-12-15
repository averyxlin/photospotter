package com.example.usecases

import net.codebot.models.LikeDTO

class MockLikeService: ILikeService {
    override suspend fun createLike(dto: LikeDTO) : LikeDTO? {
        return if (dto == validLike) {
            validLike
        } else {
            null
        }
    }

    override suspend fun deleteLike(dto: LikeDTO): LikeDTO?  {
        return if (dto == validLike) {
            validLike
        } else {
            null
        }
    }
}