package ru.echoapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt ASC")
    suspend fun between(start: Long, end: Long): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE transcript LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE anniversaryShown = 0 AND anniversaryAt <= :now")
    suspend fun dueAnniversaries(now: Long): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE anniversaryShown = 0 AND anniversaryAt <= :now AND (sealedUntil IS NULL OR sealedUntil <= :now)")
    suspend fun dueUnsealedAnniversaries(now: Long): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE createdAt < :before")
    suspend fun olderThan(before: Long): List<RecordingEntity>

    @Query("UPDATE recordings SET anniversaryShown = 1 WHERE id = :id")
    suspend fun markAnniversaryShown(id: Long)

    @Query("UPDATE recordings SET transcript = :text WHERE id = :id")
    suspend fun updateTranscript(id: Long, text: String)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertReply(reply: ReplyEntity): Long

    @Query("SELECT * FROM replies WHERE rootRecordingId = :rootId ORDER BY createdAt ASC")
    suspend fun repliesFor(rootId: Long): List<ReplyEntity>

    @Query("UPDATE replies SET transcript = :text WHERE id = :id")
    suspend fun updateReplyTranscript(id: Long, text: String)

    @Query("DELETE FROM replies WHERE id = :id")
    suspend fun deleteReplyById(id: Long)
}
