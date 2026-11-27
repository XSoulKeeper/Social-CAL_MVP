package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

/**
 * ADR 2: we do not build our own calendar – we hand the event to the
 * system calendar app with standard Intents.
 */
object CalendarHelper {

    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    fun startMillis(event: EventEntity): Long =
        EventFormats.startMillis(event.date, event.time) ?: System.currentTimeMillis()

    /** Opens the calendar's "new event" screen pre-filled with the event. */
    fun buildInsertIntent(event: EventEntity): Intent {
        val start = startMillis(event)
        val allDay = event.time == null
        val end = if (allDay) start + ONE_DAY_MS else start + AppConfig.DEFAULT_EVENT_DURATION_MS
        return Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
            .putExtra(CalendarContract.Events.TITLE, event.title)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
            .putExtra(CalendarContract.Events.DESCRIPTION, event.description)
    }

    /** Opens the calendar app on the day/time of the event. */
    fun buildViewIntent(event: EventEntity): Intent {
        val uri = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(startMillis(event).toString())
            .build()
        return Intent(Intent.ACTION_VIEW).setData(uri)
    }

    /** Self-test: is there any app that can handle ACTION_INSERT for events? */
    fun isCalendarAvailable(context: Context): Boolean = try {
        Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .resolveActivity(context.packageManager) != null
    } catch (e: Exception) {
        false
    }
}
