package ru.echoapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.echoapp.util.AnniversaryDates
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Импорт архива, созданного ExportHelper: аудио (*.m4a) + metadata.json.
 * Файлы копируются во внутреннее хранилище, записи вставляются через insert-ignore.
 */
object ImportHelper {

    data class Result(val imported: Int, val skippedDuplicates: Int)

    /**
     * Разбирает zip по [uri] (SAF) и импортирует содержимое.
     * Возвращает null при некорректном архиве/метаданных.
     */
    suspend fun import(context: Context, uri: Uri, repository: RecordingRepository): Result? =
        withContext(Dispatchers.IO) {
            runCatching {
                val audioDir = File(context.filesDir, "recordings").apply { mkdirs() }
                val audioFiles = LinkedHashMap<String, File>() // имя в архиве -> файл на диске

                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input.buffered()).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory && entry.name.startsWith("audio/")) {
                                val name = File(entry.name).name
                                val target = File(audioDir, name)
                                FileOutputStream(target).use { out -> zip.copyTo(out) }
                                audioFiles[name] = target
                            }
                            entry = zip.nextEntry
                        }
                    }
                } ?: return@runCatching null

                val json = readMetadata(context, uri) ?: return@runCatching null
                val root = JSONObject(json)

                val recordings = root.optJSONArray("recordings")?.let { array ->
                    (0 until array.length()).mapNotNull { i ->
                        parseRecording(array.getJSONObject(i), audioFiles)
                    }
                } ?: emptyList()

                val replies = root.optJSONArray("replies")?.let { array ->
                    (0 until array.length()).mapNotNull { i ->
                        parseReply(array.getJSONObject(i), audioFiles)
                    }
                } ?: emptyList()

                if (recordings.isEmpty() && replies.isEmpty()) return@runCatching null

                val (recIns, recSkip) = repository.importRecordings(recordings)
                val (repIns, repSkip) = repository.importReplies(replies)
                Result(
                    imported = recIns + repIns,
                    skippedDuplicates = recSkip + repSkip,
                )
            }.getOrNull()
        }

    private fun parseRecording(obj: JSONObject, audioFiles: Map<String, File>): RecordingEntity? {
        val name = obj.optString("file")
        val file = audioFiles[name] ?: return null
        val createdAt = obj.optLong("createdAt", -1L)
        if (createdAt <= 0) return null
        return RecordingEntity(
            id = obj.optLong("id", 0L),
            filePath = file.absolutePath,
            createdAt = createdAt,
            durationMs = obj.optLong("durationMs"),
            anniversaryAt = AnniversaryDates.plusYears(createdAt, 1),
            mood = obj.optInt("mood", -1),
            transcript = obj.optString("transcript").takeIf { it.isNotBlank() },
            sealedUntil = if (obj.has("sealedUntil") && !obj.isNull("sealedUntil")) {
                obj.optLong("sealedUntil")
            } else null,
            sealedNoDelete = obj.optBoolean("sealedNoDelete", false),
        )
    }

    private fun parseReply(obj: JSONObject, audioFiles: Map<String, File>): ReplyEntity? {
        val name = obj.optString("file")
        val file = audioFiles[name] ?: return null
        val createdAt = obj.optLong("createdAt", -1L)
        if (createdAt <= 0) return null
        return ReplyEntity(
            id = obj.optLong("id", 0L),
            rootRecordingId = obj.optLong("rootRecordingId", 0L),
            parentReplyId = if (obj.has("parentReplyId") && !obj.isNull("parentReplyId")) {
                obj.optLong("parentReplyId")
            } else null,
            filePath = file.absolutePath,
            createdAt = createdAt,
            durationMs = obj.optLong("durationMs"),
            mood = obj.optInt("mood", -1),
            transcript = obj.optString("transcript").takeIf { it.isNotBlank() },
        )
    }

    /** metadata.json читаем отдельным проходом: ZipInputStream однопроходный. */
    private fun readMetadata(context: Context, uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "metadata.json") {
                        return zip.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zip.nextEntry
                }
                null
            }
        }
}
