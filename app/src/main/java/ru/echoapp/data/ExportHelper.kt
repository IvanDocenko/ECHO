package ru.echoapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Экспорт всех записей в zip: аудио + json с метаданными. */
object ExportHelper {

    suspend fun export(context: Context, recordings: List<RecordingEntity>, replies: List<ReplyEntity>): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                // Кэш, а не Downloads: файл сразу отдаётся через share-sheet (FileProvider).
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val zipFile = File(dir, "echo_export_${System.currentTimeMillis()}.zip")

                ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                    recordings.forEach { rec ->
                        val audio = File(rec.filePath)
                        if (audio.exists()) {
                            zip.putNextEntry(ZipEntry("audio/${audio.name}"))
                            audio.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                    replies.forEach { reply ->
                        val audio = File(reply.filePath)
                        if (audio.exists()) {
                            zip.putNextEntry(ZipEntry("audio/${audio.name}"))
                            audio.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                    zip.putNextEntry(ZipEntry("metadata.json"))
                    zip.write(buildJson(recordings, replies).toByteArray())
                    zip.closeEntry()
                }
                zipFile
            }.getOrNull()
        }

    private fun esc(s: String?): String =
        (s ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun buildJson(recordings: List<RecordingEntity>, replies: List<ReplyEntity>): String {
        val sb = StringBuilder()
        sb.append("{\"version\":2,\"exportedAt\":${System.currentTimeMillis()},\"recordings\":[")
        sb.append(recordings.joinToString(",") { rec ->
            "{\"id\":${rec.id},\"file\":\"${File(rec.filePath).name}\"," +
                "\"createdAt\":${rec.createdAt},\"durationMs\":${rec.durationMs}," +
                "\"mood\":${rec.mood},\"transcript\":\"${esc(rec.transcript)}\"," +
                "\"sealedUntil\":${rec.sealedUntil ?: "null"}" +
                (if (rec.sealedNoDelete) ",\"sealedNoDelete\":true" else "") +
                "}"
        })
        sb.append("],\"replies\":[")
        sb.append(replies.joinToString(",") { reply ->
            "{\"id\":${reply.id},\"rootRecordingId\":${reply.rootRecordingId}," +
                "\"parentReplyId\":${reply.parentReplyId ?: "null"}," +
                "\"file\":\"${File(reply.filePath).name}\",\"createdAt\":${reply.createdAt}," +
                "\"durationMs\":${reply.durationMs},\"mood\":${reply.mood}," +
                "\"transcript\":\"${esc(reply.transcript)}\"}"
        })
        sb.append("]}")
        return sb.toString()
    }
}
