package com.example.database

import net.codebot.models.CreateTagDTO
import net.codebot.models.TagDTO
import net.codebot.models.UserDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class TagRepository {
    private val pinRepository = PinRepository()

    suspend fun getAllTagOptions() : List<TagDTO> {
        return dbQuery {
            Tags.selectAll()
                .map { TagDTO(
                    it[Tags.id],
                    it[Tags.name],
                ) }
        }
    }

    suspend fun createPinTag(pinId: Int, tagId: Int) {
        return dbQuery {
            PinTags.insert {
                it[PinTags.pinId] = pinId
                it[PinTags.tagId] = tagId
            }
        }
    }

    suspend fun getPinTag(pinId: Int, tagId: Int) : TagDTO? {
        return try {
         dbQuery {
            return@dbQuery PinTags.join(Tags, JoinType.INNER, PinTags.tagId, Tags.id)
                .select { (PinTags.pinId eq pinId) and (PinTags.tagId eq tagId) }
                .map { TagDTO(
                    it[PinTags.tagId],
                    it[Tags.name],
                ) }
                .single()
        }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    suspend fun createTag(name: String): Int {
        return dbQuery {
            Tags.insert {
                it[Tags.name] = name
            }[Tags.id]
        }
    }

    suspend fun getTagIdByName(name: String): Int? {
        return try {
            dbQuery {
            val tag = Tags.select { Tags.name eq name }
                .map { TagDTO(
                    it[Tags.id],
                    it[Tags.name],
                ) }
                .single()
            return@dbQuery tag.tagId
        }
    } catch (e: NoSuchElementException) {
        null
    }
    }

    suspend fun deletePinTag(pinId: Int, tagId: Int) {
        dbQuery {
            PinTags.deleteWhere {
                (PinTags.pinId eq pinId) and
                        (PinTags.tagId eq tagId)
            }
            // delete tag if no other relations exist
            if (pinRepository.getAllPins(tagId).count() == 1) {
                deleteTag(tagId)
            }
        }
    }

    suspend fun deleteTag(tagId: Int) {
        dbQuery {
            Tags.deleteWhere {
                (id eq tagId)
            }

        }
    }

    suspend fun getPinTags(pinId: Int): List<TagDTO> {
        return dbQuery {
            PinTags.join(Tags, JoinType.INNER, PinTags.tagId, Tags.id)
                .select(PinTags.pinId eq pinId)
                .map { TagDTO(
                    it[Tags.id],
                    it[Tags.name],
                    pinId,
                ) }
        }
    }
}