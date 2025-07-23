package top.anorak01.wearcord.presentation

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable


@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Message (
    val id: String,
    val content: String,
    val author: Author,
    val type: Int,
    val flags: Int,
    val attachments: List<Attachment> = emptyList(),
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Guild (
    val id: String,
    val name: String,
    val icon: String?
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Channel (
    val id: String,
    var name: String? = null,
    val type: Int,
    val guild_id: String? = null,
    val parent_id: String? = null,
    val position: Int? = null,
    val recipients: List<Author> = emptyList(), // for dms
    val last_message_id: String? = null, // for dms
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Author (
    val id: String,
    val username: String,
    val global_name: String? = null,
    val avatar: String?
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Attachment (
    val id: String,
    val filename: String,
    val size: Int,
    val url: String,
    val content_type: String,
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Me (
    val id: String,
    val username: String,
    val avatar: String?,
    val global_name: String?,
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LoginReply (
    val user_id: String,
    val mfa: Boolean,
    val ticket: String, // the token itself
    val webauthn: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MFAReply (
    val token: String,
)