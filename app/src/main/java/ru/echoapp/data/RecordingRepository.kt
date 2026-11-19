package ru.echoapp.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import ru.echoapp.util.AnniversaryDates
import ru.echoapp.work.WorkScheduler
import java.io.File

class RecordingRepository(
    private val appContext: Context,
    private val dao: RecordingDao,
) {

    val recordings: Flow<List<RecordingEntity>> = dao.observeAll()

    suspend fun saveRecording(
        file: File,
        durationMs: Long,
        mood: Int = -1,
        sealedUntil: Long? = null,
        sealedNoDelete: Boolean = false,
        waveform: String? = null,
    ): RecordingEntity {
        val createdAt = System.currentTimeMillis()
        val entity = RecordingEntity(
            filePath = file.absolutePath,
            createdAt = createdAt,
            durationMs = durationMs,
            anniversaryAt = AnniversaryDates.plusYears(createdAt, 1),
            mood = mood,
            sealedUntil = sealedUntil,
            sealedNoDelete = sealedNoDelete && sealedUntil != null,
            waveform = waveform,
        )
        val id = dao.insert(entity)
        WorkScheduler.scheduleAnniversary(appContext, id, entity.anniversaryAt)
        ru.echoapp.widget.RecordWidgetProvider.updateAll(appContext)
        return entity.copy(id = id)
    }

    /** Сохраняет текстовое письмо за год; старое письмо этого года заменяется. */
    suspend fun saveYearLetter(text: String, now: Long = System.currentTimeMillis()): RecordingEntity {
        val (start, end) = AnniversaryDates.yearBounds(now)
        dao.deleteYearLetters(start, end)
        val createdAt = System.currentTimeMillis()
        val entity = RecordingEntity(
            filePath = "",
            createdAt = createdAt,
            durationMs = 0,
            anniversaryAt = AnniversaryDates.plusYears(createdAt, 1),
            kind = "letter",
            transcript = text,
        )
        val id = dao.insert(entity)
        return entity.copy(id = id)
    }

    suspend fun yearLetter(now: Long = System.currentTimeMillis()): RecordingEntity? {
        val (start, end) = AnniversaryDates.yearBounds(now)
        return dao.yearLetter(start, end)
    }

    suspend fun updateWaveform(recordingId: Long, waveform: String) =
        dao.updateWaveform(recordingId, waveform)

    /**
     * Импорт записей: insert-ignore по id. Возвращает пару (импортировано, пропущено дубликатов).
     * Файлы импортируются во внутреннее хранилище до вызова (см. ImportHelper).
     */
    suspend fun importRecordings(entities: List<RecordingEntity>): Pair<Int, Int> {
        var inserted = 0
        var skipped = 0
        entities.forEach { entity ->
            if (dao.insertIgnore(entity) == -1L) skipped++ else {
                inserted++
                WorkScheduler.scheduleAnniversary(appContext, entity.id, entity.anniversaryAt)
            }
        }
        return inserted to skipped
    }

    suspend fun importReplies(replies: List<ReplyEntity>): Pair<Int, Int> {
        var inserted = 0
        var skipped = 0
        replies.forEach { reply ->
            if (dao.insertReplyIgnore(reply) == -1L) skipped++ else inserted++
        }
        return inserted to skipped
    }

    suspend fun saveReply(
        rootRecordingId: Long,
        parentReplyId: Long?,
        file: File,
        durationMs: Long,
        mood: Int = -1,
    ): ReplyEntity {
        val entity = ReplyEntity(
            rootRecordingId = rootRecordingId,
            parentReplyId = parentReplyId,
            filePath = file.absolutePath,
            createdAt = System.currentTimeMillis(),
            durationMs = durationMs,
            mood = mood,
        )
        val id = dao.insertReply(entity)
        return entity.copy(id = id)
    }

    fun search(query: String): Flow<List<RecordingEntity>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    suspend fun findById(id: Long): RecordingEntity? = dao.getById(id)

    suspend fun recordingsBetween(start: Long, end: Long): List<RecordingEntity> =
        dao.between(start, end)

    suspend fun allRecordingsOnce(): List<RecordingEntity> = dao.getAllOnce()

    suspend fun allRepliesOnce(): List<ReplyEntity> =
        allRecordingsOnce().flatMap { dao.repliesFor(it.id) }

    suspend fun repliesFor(rootId: Long): List<ReplyEntity> = dao.repliesFor(rootId)

    suspend fun updateTranscript(recordingId: Long, text: String) =
        dao.updateTranscript(recordingId, text)

    suspend fun updateReplyTranscript(replyId: Long, text: String) =
        dao.updateReplyTranscript(replyId, text)

    /** true, если запись удалена; false — если она запечатана «навсегда» и удаление запрещено. */
    suspend fun deleteRecording(recording: RecordingEntity, now: Long = System.currentTimeMillis()): Boolean {
        if (recording.sealedNoDelete && (recording.sealedUntil ?: 0L) > now) return false
        dao.deleteById(recording.id)
        runCatching { File(recording.filePath).delete() }
        WorkScheduler.cancelAnniversary(appContext, recording.id)
        ru.echoapp.widget.RecordWidgetProvider.updateAll(appContext)
        // Удаляем и цепочку ответов.
        repliesFor(recording.id).forEach { reply ->
            dao.deleteReplyById(reply.id)
            runCatching { File(reply.filePath).delete() }
        }
        return true
    }

    suspend fun deleteReply(reply: ReplyEntity) {
        dao.deleteReplyById(reply.id)
        runCatching { File(reply.filePath).delete() }
    }

    /** Удаляет записи и ответы старше [cutoff]; возвращает число удалённых записей. */
    suspend fun cleanupOlderThan(cutoff: Long): Int {
        val old = dao.olderThan(cutoff)
        old.forEach { deleteRecording(it) }
        return old.size
    }

    /** Случайная запись прошлого для «записи недели». */
    suspend fun randomPastRecording(minAgeMs: Long, now: Long = System.currentTimeMillis()): RecordingEntity? {
        val candidates = dao.between(0, now - minAgeMs).filter { it.sealedUntil == null || it.sealedUntil <= now }
        return candidates.randomOrNull()
    }

    /** Показывает уведомления по годовщинам, которые наступили, пока приложение не работало. */
    suspend fun processMissedAnniversaries(now: Long = System.currentTimeMillis()) {
        dao.dueUnsealedAnniversaries(now).forEach { recording ->
            WorkScheduler.notifyAnniversary(appContext, recording)
            dao.markAnniversaryShown(recording.id)
        }
    }
}
