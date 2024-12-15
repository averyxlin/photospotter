package com.example.usecases

import net.codebot.models.CreateTagDTO
import net.codebot.models.TagDTO

interface ITagService {
    suspend fun getAllTagOptions() : List<TagDTO>
    suspend fun createPinTag(dto: CreateTagDTO) : TagDTO?
    suspend fun deletePinTag(pinId: Int, tagId: Int) : TagDTO?
}