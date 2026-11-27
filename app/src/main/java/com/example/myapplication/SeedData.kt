package com.example.myapplication

/**
 * ============================================================================
 *  MANUAL DATABASE ENTRIES  (Event History / "biblioteka aplikacji")
 * ============================================================================
 *
 * Add, edit or remove entries below. They are inserted into the local Room
 * database (events.db) when the database is created (EventDatabase.kt -> SeedCallback).
 *
 * After editing this list, apply the changes in ONE of these ways:
 *   a) set AppConfig.RESEED_DATABASE_ON_START = true  (DB refilled on every start), or
 *   b) bump EventDatabase.DB_VERSION by 1              (DB recreated once), or
 *   c) uninstall the app / "Clear storage" in Android settings.
 *
 * Field formats:
 *   date  = "yyyy-MM-dd"         e.g. "2026-12-31"
 *   time  = "HH:mm" or null      e.g. "19:30"  (null = all-day event)
 *   saved = true  -> History shows "Saved" + "View in calendar"
 *           false -> History shows "Not saved" + "Add to calendar"
 *   thumbnailPath = null -> a placeholder icon is shown
 *
 * Tip: you can also edit rows live while the app runs:
 *   Android Studio -> View -> Tool Windows -> App Inspection -> Database Inspector -> events.db
 */
object SeedData {

    val ENTRIES: List<EventEntity> = listOf(
        EventEntity(
            title = "Bus Drivers Link Up",
            date = "2025-07-06",
            time = "19:00",
            location = "Addington Park, Kent Gateway",
            description = "Summer all white festival",
            saved = true,
        ),
        EventEntity(
            title = "Jazz Night at the Riverside",
            date = "2026-12-12",
            time = "20:30",
            location = "Riverside Club",
            description = "Live jazz quartet",
            saved = true,
        ),
        EventEntity(
            title = "Student Hackathon",
            date = "2027-01-16",
            time = "09:00",
            location = "Campus Hall B",
            description = "24h coding marathon",
            saved = true,
        ),
        EventEntity(
            title = "Winter Book Fair",
            date = "2026-12-05",
            time = null,
            location = "City Library",
            description = "All-day event",
            saved = true,
        ),
        EventEntity(
            title = "Charity Run 5K",
            date = "2027-03-21",
            time = "10:00",
            location = "Central Park",
            description = "Not added to the calendar yet",
            saved = false,
        ),
    )
}
