package ru.echoapp.util

/** Пять эмодзи-меток настроения, индекс = значение mood в БД. */
object Mood {
    val emojis = listOf("😔", "😐", "🙂", "😄", "🔥")

    fun emoji(index: Int): String = emojis.getOrElse(index) { "" }

    fun isValid(index: Int): Boolean = index in emojis.indices
}
