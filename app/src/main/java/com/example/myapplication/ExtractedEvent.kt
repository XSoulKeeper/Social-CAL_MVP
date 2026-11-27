package com.example.myapplication

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Structured data extracted from a poster (what the LLM returns). */
data class ExtractedEvent(
    val title: String,
    /** ISO date: yyyy-MM-dd */
    val date: String,
    /** 24h time: HH:mm, null = all-day */
    val time: String?,
    val location: String,
    val description: String,
)

/** Editable form on the "Wrap up" screen (display formats: dd.MM.yyyy / 7:00 PM). */
data class WrapUpForm(
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val description: String,
) {
    /** Converts user-editable text back to the ISO model. Throws [ScanException.Validation]. */
    fun toExtractedEvent(): ExtractedEvent {
        val isoDate = EventFormats.displayToIsoDate(date)
            ?: throw ScanException.Validation("Date must look like 06.07.2025 (dd.MM.yyyy).")
        val isoTime = if (time.isBlank()) {
            null
        } else {
            EventFormats.displayToIsoTime(time)
                ?: throw ScanException.Validation("Time must look like 7:00 PM or 19:00.")
        }
        return ExtractedEvent(
            title = title.trim(),
            date = isoDate,
            time = isoTime,
            location = location.trim(),
            description = description.trim(),
        )
    }

    companion object {
        val EMPTY = WrapUpForm("", "", "", "", "")

        fun from(event: ExtractedEvent) = WrapUpForm(
            title = event.title,
            date = EventFormats.isoToDisplayDate(event.date) ?: event.date,
            time = event.time?.let { EventFormats.isoToDisplayTime(it) ?: it } ?: "",
            location = event.location,
            description = event.description,
        )
    }
}

/** Date / time helpers (java.text, works from minSdk 24 and in JVM unit tests). */
object EventFormats {
    const val ISO_DATE = "yyyy-MM-dd"
    const val ISO_TIME = "HH:mm"
    const val DISPLAY_DATE = "dd.MM.yyyy"
    const val DISPLAY_TIME = "h:mm a"

    private val ISO_TIME_INPUTS = listOf("HH:mm", "H:mm")
    private val DISPLAY_TIME_INPUTS = listOf("h:mm a", "h:mma", "H:mm")

    /** Parses the whole string strictly (no lenient dates like 31.02, no trailing text). */
    fun parseStrict(pattern: String, text: String): Date? {
        val input = text.trim()
        if (input.isEmpty()) return null
        val format = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val result = format.parse(input, position) ?: return null
        return if (position.index == input.length) result else null
    }

    fun format(pattern: String, date: Date): String =
        SimpleDateFormat(pattern, Locale.US).format(date)

    fun parseIsoDate(text: String): Date? = parseStrict(ISO_DATE, text)

    fun parseIsoTime(text: String): Date? =
        ISO_TIME_INPUTS.firstNotNullOfOrNull { parseStrict(it, text) }

    fun isoToDisplayDate(iso: String): String? = parseIsoDate(iso)?.let { format(DISPLAY_DATE, it) }

    fun isoToDisplayTime(iso: String): String? = parseIsoTime(iso)?.let { format(DISPLAY_TIME, it) }

    fun displayToIsoDate(display: String): String? =
        parseStrict(DISPLAY_DATE, display)?.let { format(ISO_DATE, it) }

    fun displayToIsoTime(display: String): String? =
        DISPLAY_TIME_INPUTS.firstNotNullOfOrNull { parseStrict(it, display) }?.let { format(ISO_TIME, it) }

    /** Event start in epoch millis (device time zone). All-day events start at 00:00. */
    fun startMillis(isoDate: String, isoTime: String?): Long? {
        val date = parseIsoDate(isoDate) ?: return null
        val calendar = Calendar.getInstance().apply { time = date }
        if (isoTime != null) {
            val time = parseIsoTime(isoTime) ?: return null
            val timeCalendar = Calendar.getInstance().apply { this.time = time }
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
        }
        return calendar.timeInMillis
    }
}
