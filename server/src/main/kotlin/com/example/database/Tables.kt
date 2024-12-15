package com.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Pins : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 50)
    val address = varchar("address", length = 200)
    val lat = double("lat")
    val lon = double("lon")
    val userId = reference("userId", Users.id, onDelete = ReferenceOption.CASCADE)
    val desc = text("desc").nullable()
    val createdAt = text("createdAt").nullable()

    override val primaryKey = PrimaryKey(id)
}

object PinTags : Table() {
    val pinId = reference("pinId", Pins.id, onDelete = ReferenceOption.CASCADE)
    val tagId = reference("tagId", Tags.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(pinId, tagId)
}

object Tags : Table() {
    val id = integer("id").autoIncrement()
    val name = text("name")

    override val primaryKey = PrimaryKey(id)
}

object Comments : Table() {
    val id = integer("id").autoIncrement()
    val content = text("content")
    val userId = reference("userId", Users.id, onDelete = ReferenceOption.CASCADE)
    val pinId = reference("pinId", Pins.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = text("createdAt")

    override val primaryKey = PrimaryKey(id)
}

object Photos : Table() {
    val id = integer("id").autoIncrement()
    val photoURL = varchar("photoURL", length = 200)
    val commentId = reference("commentId", Comments.id, onDelete = ReferenceOption.CASCADE).nullable()
    val pinId = reference("pinId", Pins.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 50)
    val email = varchar("email", length = 200)
    val authId = varchar("authId", length = 200).nullable()
    val bioPhoto = varchar("bioPhoto", length = 200).nullable()
    val bio = text("bio").nullable()
    val igURL = varchar("igURL", length = 200).nullable()
    val siteURL = varchar("siteURL", length = 200).nullable()

    override val primaryKey = PrimaryKey(id)
}

object UsersFollowing : Table() {
    val followerId = reference("followerId", Users.id, onDelete = ReferenceOption.CASCADE)
    val followeeId = reference("followeeId", Users.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(followerId, followeeId)
}

object UserPinLikes : Table() {
    val userId = reference("userId", Users.id, onDelete = ReferenceOption.CASCADE)
    val pinId = reference("pinId", Pins.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(pinId, userId)
}

object UserBookmarkedPins : Table() {
    val userId = reference("userId", Users.id, onDelete = ReferenceOption.CASCADE)
    val pinId = reference("pinId", Pins.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(pinId, userId)
}
