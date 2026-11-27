package com.example.myapplication

import android.content.ContentValues
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ROOM ENTITY = one table in the database.
 *
 * Room reads this class at compile time (KSP) and generates the SQL for it:
 *   CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, ...)
 *
 * - every constructor property = one column
 * - String  -> TEXT NOT NULL,  String? -> TEXT (nullable)
 * - Boolean -> INTEGER (0/1),   Long    -> INTEGER
 * - @ColumnInfo(name = ...) only changes the column name in SQL
 *
 * Adding a column: add a property here + bump EventDatabase.DB_VERSION.
 */
@Entity(tableName = EventEntity.TABLE)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    /** yyyy-MM-dd */
    val date: String,
    /** HH:mm or null (all-day) */
    val time: String? = null,
    val location: String = "",
    val description: String = "",
    /** true = added to the system calendar */
    val saved: Boolean = false,
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TABLE = "events"

        fun fromExtracted(event: ExtractedEvent, thumbnailPath: String?) = EventEntity(
            title = event.title,
            date = event.date,
            time = event.time,
            location = event.location,
            description = event.description,
            thumbnailPath = thumbnailPath,
        )
    }
}

// Display helpers are extension properties (outside the class) so Room
// does not treat them as columns.

val EventEntity.displayDate: String
    get() = EventFormats.isoToDisplayDate(date) ?: date

val EventEntity.displayDateTime: String
    get() = displayDate + (time?.let { " • " + (EventFormats.isoToDisplayTime(it) ?: it) } ?: "")

/** Used only for inserting SeedData while the database is being created (see EventDatabase). */
internal fun EventEntity.toContentValues(): ContentValues = ContentValues().apply {
    put("title", title)
    put("date", date)
    put("time", time)
    put("location", location)
    put("description", description)
    put("saved", if (saved) 1 else 0)
    put("thumbnail_path", thumbnailPath)
    put("created_at", createdAt)
}
