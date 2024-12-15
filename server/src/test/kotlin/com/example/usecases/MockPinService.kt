package com.example.usecases

import net.codebot.models.CreatePinDTO
import net.codebot.models.PinDTO

class MockPinService: IPinService {
    override suspend fun getAllPins(tagId: Int?) : List<PinDTO> {
        return listOf()
    }

    override suspend fun getPin(pinId: Int) : PinDTO? {
        return if (pinId == existingPin.id) {
            existingPin
        } else {
            null
        }
    }

    override suspend fun getPinsByUserId(userId: Int) : List<PinDTO> {
        return listOf()
    }

    override suspend fun getPinsFromFollowing(userId: Int) : List<PinDTO> {
        return listOf()
    }

    override suspend fun createPin(dto: CreatePinDTO) : PinDTO? {
        return if (dto == validCreatePinDTO) {
            existingPin
        } else {
            null
        }
    }

    override suspend fun updatePin(id: Int, pin: PinDTO) : PinDTO? {
        return if (pin == existingPin) {
            existingPin
        } else {
            null
        }
    }

    override suspend fun deletePin(pinId: Int): PinDTO? {
        return if (pinId == existingPin.id) {
            existingPin
        } else {
            null
        }
    }
}