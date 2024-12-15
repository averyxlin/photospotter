package net.codebot.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

@Serializable
data class CreateUserDTO(
    val name: String,
    val email: String,
    val authId: String,
    val bioPhoto: String? = null,
    val bio: String? = null,
    val igURL: String? = null,
    val siteURL: String? = null,
)

@Serializable
data class UserDTO(
    val id: Int,
    val name: String,
    val email: String,
    val authId: String? = null,
    val bioPhoto: String? = null,
    val bio: String? = null,
    val igURL: String? = null,
    val siteURL: String? = null,
    val followerCount: Int? = null,
    val followingCount: Int? = null,
)
@Serializable
data class WeatherDTO(
    val pinId: Int,
    val desc: String,
    val temp: Int,
    val humidity: Float,
    val windSpeed: Float,
    val sunriseTime: String,
    val sunsetTime: String,
    val precipitation: Float,
)
@Serializable
data class CommentDTO(
    val id: Int,
    val pinId: Int,
    val content: String,
    val userId: Int,
    val photos: List<String>? = null,
    val createdAt: String,
)

@Serializable
data class CreateCommentDTO(
    val pinId: Int,
    val content: String,
    val userId: Int,
    val photos: List<String>? = null,
)
@Serializable
data class PinDTO(
    val id: Int,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val userId: Int,
    val desc: String? = null,
    val createdAt: String? = null,
    val user: UserDTO? = null,
    val tags: List<TagDTO>? = null,
    val photos: List<String>? = null,
    val likes: Int? = null,
    val comments: List<CommentDTO>? = null,
    val likedByUserIds: List<Int>? = null,
)

@Serializable
data class CreatePinDTO(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val userId: Int,
    val desc: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val tags: List<CreateTagDTO>? = null,
    val photos: List<String>? = null,
)

@Serializable
data class UpdateBookmarkDTO(
    val pinId: Int,
    val userId: Int,
)

@Serializable
data class TagDTO(
    val tagId: Int,
    val name: String,
    val pinId: Int? = null,
)

@Serializable
data class CreateTagDTO(
    val name: String,
    val pinId: Int? = null,
)

@Serializable
data class LikeDTO(
    val pinId: Int,
    val userId: Int,
)

@Serializable
data class UpdateFollowingDTO(
    val followerId: Int,
    val followeeId: Int,
)