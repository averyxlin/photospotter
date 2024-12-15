package com.example.usecases

import com.example.database.MockDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TagServiceTest {
    private val tagService = TagService(MockDatabase())

    @Test
    fun getAllTagOptions() = runBlocking{
        assertEquals(listOf(existingTag), tagService.getAllTagOptions())
    }
    @Test
    fun createPinTag() = runBlocking{
        assertEquals(existingTag, tagService.createPinTag(validCreateTagDTO))
        assertEquals(null, tagService.createPinTag(invalidCreateTagDTO))
        assertEquals(null, tagService.createPinTag(createTagNoPin))
    }
    @Test
    fun deletePinTag() = runBlocking{
        assertEquals(existingTag, tagService.deletePinTag(existingTag.pinId!!, existingTag.tagId))
        assertEquals(null, tagService.deletePinTag(newTag.pinId!!, newTag.tagId))
    }
}