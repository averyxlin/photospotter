package com.example.usecases

import com.example.database.MockDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BookmarkServiceTest {
    private val bookmarkService = BookmarkService(MockDatabase())

    @Test
    fun createBookmark() = runBlocking{
        assertEquals(existingBookmark, bookmarkService.createBookmark(existingBookmark))
        assertEquals(null, bookmarkService.createBookmark(invalidBookmark))
    }
    @Test
    fun getBookmarks() = runBlocking{
        assertEquals(listOf(fullPinDTO), bookmarkService.getBookmarks(existingUser.id))
        assertEquals(listOf(), bookmarkService.getBookmarks(newUser.id))
    }

    @Test
    fun deleteBookmark() = runBlocking{
        assertEquals(existingBookmark, bookmarkService.deleteBookmark(existingBookmark))
        assertEquals(null, bookmarkService.deleteBookmark(invalidBookmark))
    }
}