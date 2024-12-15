package com.example.usecases

import net.codebot.models.LikeDTO

interface ILikeService {
    suspend fun createLike(dto: LikeDTO) : LikeDTO?
    suspend fun deleteLike(dto: LikeDTO): LikeDTO?
}