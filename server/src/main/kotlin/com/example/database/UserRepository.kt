package com.example.database

import net.codebot.models.CreateUserDTO
import net.codebot.models.UserDTO
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class UserRepository {
    suspend fun getUser(userId: Int) : UserDTO? {
        return try {
            dbQuery {
                val user = Users.select { Users.id eq userId }
                    .map { UserDTO(
                        it[Users.id],
                        it[Users.name],
                        it[Users.email],
                        it[Users.authId],
                        it[Users.bioPhoto],
                        it[Users.bio],
                        it[Users.igURL],
                        it[Users.siteURL],
                    ) }.single()
                return@dbQuery user
            }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    suspend fun getUserByAuthId(authId: String) : UserDTO? {
        return try {
            dbQuery {
                Users.select { Users.authId eq authId }
                    .map { UserDTO(
                        it[Users.id],
                        it[Users.name],
                        it[Users.email],
                        it[Users.authId],
                        it[Users.bioPhoto],
                        it[Users.bio],
                        it[Users.igURL],
                        it[Users.siteURL],
                    ) }.single()
            }
        } catch (e: NoSuchElementException) {
            null
        }
    }


    suspend fun createUser(user: CreateUserDTO) : Int {
        val newId = dbQuery {
            Users.insert {
                it[name] = user.name
                it[email] = user.email
                it[authId] = user.authId
                it[bioPhoto] = user.bioPhoto
                it[bio] = user.bio
                it[igURL] = user.igURL
                it[siteURL] = user.siteURL
            }[Users.id]
        }
        return newId
    }
    suspend fun updateUser(id: Int, user: UserDTO) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[name] = user.name
                it[email] = user.email
                it[authId] = user.authId
                it[bioPhoto] = user.bioPhoto
                it[bio] = user.bio
                it[igURL] = user.igURL
                it[siteURL] = user.siteURL
            }
        }
    }

    suspend fun deleteUser(userId: Int) {
        dbQuery {
            Users.deleteWhere { id.eq(userId) }
        }
    }
}