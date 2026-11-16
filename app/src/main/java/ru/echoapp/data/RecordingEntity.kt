package ru.echoapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val createdAt: Long,
    val durationMs: Long,
    /** Момент, когда запись станет «письмом из прошлого» (createdAt + 1 год). */
    val anniversaryAt: Long,
    /** Показано ли уже уведомление-годовщина. */
    val anniversaryShown: Boolean = false,
    /** Индекс эмодзи-настроения (0..4) или -1, если не выбрано. */
    val mood: Int = -1,
    /** Транскрипция речи (может отсутствовать, если распознавание недоступно). */
    val transcript: String? = null,
    /** Если > now — запись запечатана и недоступна для прослушивания до этой даты. */
    val sealedUntil: Long? = null,
)
