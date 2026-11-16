package ru.echoapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Голосовой ответ в цепочке «оригинал → ответ → ответ на ответ». */
@Entity(tableName = "replies")
data class ReplyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Запись-основание (корень цепочки). */
    val rootRecordingId: Long,
    /** Родительский ответ; null = ответ на саму запись. */
    val parentReplyId: Long? = null,
    val filePath: String,
    val createdAt: Long,
    val durationMs: Long,
    val mood: Int = -1,
    val transcript: String? = null,
)
