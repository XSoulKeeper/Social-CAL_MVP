package com.example.myapplication

import org.json.JSONException
import org.json.JSONObject

/**
 * Turns the raw LLM answer into [ExtractedEvent].
 * Tolerates what LLMs typically do: ```json fences, text before/after the JSON object.
 * Throws [ScanException.Parse] when no valid JSON object can be found.
 */
object EventJsonParser {

    fun parse(raw: String): ExtractedEvent {
        val text = raw.trim()
        if (text.isEmpty()) throw ScanException.Parse("The model returned an empty response.")

        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw ScanException.Parse("The model response does not contain JSON.")
        }

        val json = try {
            JSONObject(text.substring(start, end + 1))
        } catch (e: JSONException) {
            throw ScanException.Parse("The model response is not valid JSON.", e)
        }

        return ExtractedEvent(
            title = json.stringOrEmpty("title"),
            date = json.stringOrEmpty("date"),
            time = json.stringOrEmpty("time").ifEmpty { null },
            location = json.stringOrEmpty("location"),
            description = json.stringOrEmpty("description"),
        )
    }

    /** optString() would turn JSON null into the text "null" – avoid that. */
    private fun JSONObject.stringOrEmpty(key: String): String =
        if (isNull(key)) "" else optString(key, "").trim()
}

/** Business rules for an extracted event. Throws [ScanException.Validation]. */
object EventValidator {

    fun validate(event: ExtractedEvent): ExtractedEvent {
        if (event.title.isBlank()) {
            throw ScanException.Validation("Event name is missing.")
        }
        if (event.date.isBlank()) {
            throw ScanException.Validation("Event date is missing.")
        }
        if (EventFormats.parseIsoDate(event.date) == null) {
            throw ScanException.Validation("Invalid date \"${event.date}\" (expected yyyy-MM-dd).")
        }
        if (event.time != null && EventFormats.parseIsoTime(event.time) == null) {
            throw ScanException.Validation("Invalid time \"${event.time}\" (expected HH:mm).")
        }
        return event
    }
}
