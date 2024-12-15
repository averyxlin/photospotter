package com.example.usecases

import net.codebot.models.CreatePinDTO
import net.codebot.models.PinDTO

interface IPinService {
    suspend fun getAllPins(tagId: Int?) : List<PinDTO>
    suspend fun getPin(pinId: Int) : PinDTO?
    suspend fun getPinsByUserId(userId: Int) : List<PinDTO>
    suspend fun getPinsFromFollowing(userId: Int) : List<PinDTO>
    suspend fun createPin(dto: CreatePinDTO) : PinDTO?
    suspend fun updatePin(id: Int, pin: PinDTO) : PinDTO?
    suspend fun deletePin(pinId: Int): PinDTO?
}