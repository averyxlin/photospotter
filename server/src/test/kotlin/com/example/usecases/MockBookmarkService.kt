package com.example.usecases

import net.codebot.models.PinDTO
import net.codebot.models.UpdateBookmarkDTO

class MockBookmarkService: IBookmarkService {
    override suspend fun createBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO? {
        if (dto == newBookmark) {
            return newBookmark
        }
        return null
    }

    override suspend fun getBookmarks(userId: Int) : List<PinDTO> {
        return listOf()
    }

    override suspend fun deleteBookmark(dto: UpdateBookmarkDTO): UpdateBookmarkDTO? {
        if (dto == existingBookmark) {
            return dto
        }
        return null
    }
}