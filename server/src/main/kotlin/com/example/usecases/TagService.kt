package com.example.usecases

import com.example.database.IDatabase
import net.codebot.models.CreateTagDTO
import net.codebot.models.TagDTO
import org.jetbrains.exposed.exceptions.ExposedSQLException

class TagService (db: IDatabase):ITagService {
    private val db: IDatabase = db

    override suspend fun getAllTagOptions() : List<TagDTO> {
        return db.getAllTagOptions()
    }

    override suspend fun createPinTag(dto: CreateTagDTO) : TagDTO? {
        if (dto.pinId == null) {
            return null
        }
        return db.transaction {
            var tagId: Int?
            tagId = try {
                db.getTagIdByName(dto.name)
            } catch (e: ExposedSQLException) {
                null
            }
            if (tagId == null) {
                tagId = db.createTag(dto.name)
            }
            db.createPinTag(dto.pinId!!, tagId)
            return@transaction try {
                db.getPinTag(dto.pinId!!, tagId);
            } catch (e: NoSuchElementException) {
                null
            }
        }
    }

    override suspend fun deletePinTag(pinId: Int, tagId: Int) : TagDTO? {
        val ret = db.getPinTag(pinId, tagId) ?: return null
        db.transaction {
            db.deletePinTag(pinId, tagId)
            // delete tag if no other relations exist
            if (db.getAllPins(tagId).count() == 1) {
                db.deleteTag(tagId)
            }
        }
        return ret
    }
}