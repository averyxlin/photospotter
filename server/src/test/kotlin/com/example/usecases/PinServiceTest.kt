package com.example.usecases

import com.example.database.MockDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PinServiceTest {
    private val pinService = PinService(MockDatabase())

    @Test
    fun getAllPins() = runBlocking{
        assertEquals(listOf(existingPin), pinService.getAllPins(null))
        assertEquals(listOf(), pinService.getAllPins(existingTag.tagId))
    }
    @Test
    fun getPin() = runBlocking{
        assertEquals(fullPinDTO, pinService.getPin(existingPin.id))
        assertEquals(null, pinService.getPin(newPin.id))
    }
    @Test
    fun getPinsByUserId() = runBlocking{
        assertEquals(listOf(fullPinDTO), pinService.getPinsByUserId(existingUser.id))
        assertEquals(listOf(), pinService.getPinsByUserId(newUser.id))
    }
    @Test
    fun getPinsFromFollowing() = runBlocking{
        assertEquals(listOf(fullPinDTO), pinService.getPinsFromFollowing(existingUser.id))
        assertEquals(listOf(), pinService.getPinsFromFollowing(newUser.id))
    }
    @Test
    fun createPin() = runBlocking{
        assertEquals(fullPinDTO, pinService.createPin(validCreatePinDTO))
        assertEquals(null, pinService.createPin(invalidCreatePinDTO))
    }
    @Test
    fun updatePin() = runBlocking{
        assertEquals(existingPin, pinService.updatePin(existingPin.id, existingPin))
        assertEquals(null, pinService.updatePin(newPin.id, newPin))
    }
    @Test
    fun deletePin() = runBlocking{
        assertEquals(existingPin, pinService.deletePin(existingPin.id))
        assertEquals(null, pinService.deletePin(newPin.id))
    }
}