package com.example.database

import net.codebot.models.CreatePinDTO
import net.codebot.models.PinDTO
import net.codebot.models.UserDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class PinRepository {
    suspend fun getAllPins(tagId: Int?) : List<PinDTO> {
        if (tagId == null) {
            return dbQuery {
                Pins.join(Users, JoinType.INNER, Pins.userId, Users.id)
                    .selectAll()
                    .map { PinDTO(
                        it[Pins.id],
                        it[Pins.name],
                        it[Pins.address],
                        it[Pins.lat],
                        it[Pins.lon],
                        it[Pins.userId],
                        it[Pins.desc],
                        it[Pins.createdAt],
                        UserDTO(
                            it[Users.id],
                            it[Users.name],
                            it[Users.email],
                            it[Users.bioPhoto],
                        ),
                    ) }
            }
        }
        else {
            return dbQuery {
                Pins.join(PinTags, JoinType.INNER, Pins.id, PinTags.pinId)
                    .join(Users, JoinType.INNER, Pins.userId, Users.id)
                    .select{ PinTags.tagId eq tagId }
                    .map { PinDTO(
                        it[Pins.id],
                        it[Pins.name],
                        it[Pins.address],
                        it[Pins.lat],
                        it[Pins.lon],
                        it[Pins.userId],
                        it[Pins.desc],
                        it[Pins.createdAt],
                        UserDTO(
                            it[Users.id],
                            it[Users.name],
                            it[Users.email],
                            it[Users.bioPhoto],
                        ),
                    ) }
            }
        }
    }

    suspend fun getPin(pinId: Int) : PinDTO? {
        return dbQuery {
            Pins.select { Pins.id eq pinId }
                .map { PinDTO(
                    it[Pins.id],
                    it[Pins.name],
                    it[Pins.address],
                    it[Pins.lat],
                    it[Pins.lon],
                    it[Pins.userId],
                    it[Pins.desc],
                    it[Pins.createdAt],
                ) }.single()
        }
    }

    suspend fun getPinsByUserId(userId: Int) : List<PinDTO> {
        return dbQuery {
            Pins.select { Pins.userId eq userId }
                .map { PinDTO(
                    it[Pins.id],
                    it[Pins.name],
                    it[Pins.address],
                    it[Pins.lat],
                    it[Pins.lon],
                    it[Pins.userId],
                    it[Pins.desc],
                    it[Pins.createdAt]
                ) }
        }
    }

    suspend fun getPinsFromFollowing(userId: Int) : List<PinDTO> {
        return dbQuery {
            UsersFollowing.join(Pins, JoinType.INNER, UsersFollowing.followeeId, Pins.userId)
                .join(Users, JoinType.INNER, Pins.userId, Users.id)
                .select(UsersFollowing.followerId eq userId)
                .orderBy(Pins.createdAt)
                .map { PinDTO(
                    it[Pins.id],
                    it[Pins.name],
                    it[Pins.address],
                    it[Pins.lat],
                    it[Pins.lon],
                    it[Pins.userId],
                    it[Pins.desc],
                    it[Pins.createdAt],
                    UserDTO(
                        it[Users.id],
                        it[Users.name],
                        it[Users.email],
                        it[Users.bioPhoto],
                    ),
                ) }
        }
    }
    suspend fun createPin(dto: CreatePinDTO) : Int {
        return dbQuery {
            val currentTimestamp = LocalDateTime.now()
            val newId = dbQuery {
                Pins.insert {
                    it[name] = dto.name
                    it[address] = dto.address
                    it[lat] = dto.lat
                    it[lon] = dto.lon
                    it[userId] = dto.userId
                    it[desc] = dto.desc
                    it[createdAt] = currentTimestamp.toString()
                }[Pins.id]
            }
            return@dbQuery newId
        }
    }
    suspend fun updatePin(id: Int, pin: PinDTO) {
        dbQuery {
            Pins.update({ Pins.id eq id }) {
                it[name] = pin.name
                it[address] = pin.address
                it[lat] = pin.lat
                it[lon] = pin.lon
                it[desc] = pin.desc
            }
        }
    }

    suspend fun deletePin(pinId: Int) {
        dbQuery {
            Pins.deleteWhere { id.eq(pinId) }
        }

    }
}