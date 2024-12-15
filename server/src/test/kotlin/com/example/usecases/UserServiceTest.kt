package com.example.usecases

import com.example.database.MockDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class UserServiceTest {
    private val userService = UserService(MockDatabase())
    @Test
    fun getUser() = runBlocking{
        assertEquals(fullUserDTO, userService.getUser(existingUser.id))
        assertEquals(null, userService.getUser(newUser.id))
    }
    @Test
    fun getUserByAuthId() = runBlocking{
        assertEquals(existingUser, userService.getUserByAuthId(existingUser.authId!!))
        assertEquals(null, userService.getUserByAuthId(newUser.authId!!))
    }
    @Test
    fun createUser() = runBlocking{
        assertEquals(existingUser, userService.createUser(validCreateUserDTO))
        assertEquals(null, userService.createUser(invalidCreateUserDTO))
    }
    @Test
    fun updateUser() = runBlocking{
        assertEquals(existingUser, userService.updateUser(existingUser.id, existingUser))
        assertEquals(null, userService.updateUser(newUser.id, newUser))
    }
    @Test
    fun deleteUser() = runBlocking{
        assertEquals(existingUser, userService.deleteUser(existingUser.id))
        assertEquals(null, userService.deleteUser(newUser.id))
    }
}