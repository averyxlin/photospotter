package com.example.database

import net.codebot.models.UpdateFollowingDTO
import net.codebot.models.UserDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class FollowRepository {
    suspend fun getFollow(dto: UpdateFollowingDTO): UpdateFollowingDTO? {
        if (dto.followerId == dto.followeeId) {
            return null
        }
        return try {
            return dbQuery {
                UsersFollowing.select { (UsersFollowing.followerId eq dto.followerId) and
                        (UsersFollowing.followeeId eq dto.followeeId)}
                    .map { UpdateFollowingDTO(
                        it[UsersFollowing.followerId],
                        it[UsersFollowing.followeeId],
                    ) }.single()
            }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    suspend fun createFollow(dto: UpdateFollowingDTO) {
        return dbQuery {
            UsersFollowing.insert {
                it[followerId] = dto.followerId
                it[followeeId] = dto.followeeId
            }
        }
    }
    suspend fun deleteFollow(dto: UpdateFollowingDTO) {
        dbQuery {
            UsersFollowing.deleteWhere {
                (followerId eq dto.followerId) and
                        (followeeId eq dto.followeeId)
            }
        }
    }
    suspend fun getFollowers(userId: Int): List<UserDTO> {
        return dbQuery {
            UsersFollowing.join(Users, JoinType.INNER, UsersFollowing.followerId, Users.id)
                .select(UsersFollowing.followeeId eq userId)
                .map { UserDTO(
                    it[Users.id],
                    it[Users.name],
                    it[Users.email],
                    it[Users.authId],
                    it[Users.bioPhoto],
                    it[Users.bio],
                    it[Users.igURL],
                    it[Users.siteURL],
                ) }
        }
    }

    suspend fun getFollowing(userId: Int): List<UserDTO> {
        return dbQuery {
            UsersFollowing.join(Users, JoinType.INNER, UsersFollowing.followeeId, Users.id)
                .select(UsersFollowing.followerId eq userId)
                .map { UserDTO(
                    it[Users.id],
                    it[Users.name],
                    it[Users.email],
                    it[Users.authId],
                    it[Users.bioPhoto],
                    it[Users.bio],
                    it[Users.igURL],
                    it[Users.siteURL],
                ) }
        }
    }
}