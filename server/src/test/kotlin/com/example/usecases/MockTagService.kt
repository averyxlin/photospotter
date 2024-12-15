package com.example.usecases

import net.codebot.models.CreateTagDTO
import net.codebot.models.TagDTO

class MockTagService: ITagService {
    override suspend fun getAllTagOptions() : List<TagDTO> {
        return listOf()
    }

    override suspend fun createPinTag(dto: CreateTagDTO) : TagDTO? {
        return if (dto == validCreateTagDTO) {
            newTag
        } else {
            null
        }
    }

    override suspend fun deletePinTag(pinId: Int, tagId: Int) : TagDTO?  {
        return if ((pinId == existingTag.pinId) && (tagId == existingTag.tagId)) {
            existingTag
        } else {
            null
        }
    }
}