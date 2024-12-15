package com.example.usecases

import net.codebot.models.PinDTO
import net.codebot.models.UpdateBookmarkDTO

interface IBookmarkService {
    suspend fun createBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO?
    suspend fun getBookmarks(userId: Int) : List<PinDTO>
    suspend fun deleteBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO?
}