package com.example.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * ROOM DATABASE = the database file (events.db) + list of tables + version.
 *
 * Three pieces work together:
 *   EventEntity.kt  – what a row looks like (table)
 *   EventDao.kt     – what you can do with rows (queries)
 *   EventDatabase   – this file: opens the database, creates tables, inserts SeedData
 *
 * Usage:  EventDatabase.get(context).eventDao().getAll()
 */
@Database(
    entities = [EventEntity::class],
    version = EventDatabase.DB_VERSION,
    exportSchema = false,
)
abstract class EventDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        const val DB_NAME = "events.db"

        /**
         * Bump by 1 when you change EventEntity (new column) or edit SeedData.kt.
         * The prototype has no migrations: a higher version deletes the old
         * database and creates a new one filled with SeedData.
         * (Version 2 = first Room version; 1 was the old hand-written SQLite.)
         */
        const val DB_VERSION = 3

        @Volatile
        private var instance: EventDatabase? = null

        /** One shared database for the whole app. */
        fun get(context: Context): EventDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext, DB_NAME).also { instance = it }
            }

        /** @param name file name, or null for an in-memory database (tests). */
        fun build(context: Context, name: String?): EventDatabase {
            val builder = if (name == null) {
                Room.inMemoryDatabaseBuilder(context, EventDatabase::class.java)
            } else {
                Room.databaseBuilder(context, EventDatabase::class.java, name)
            }
            // fallbackToDestructiveMigration: when DB_VERSION changes (up OR down) and there is
            // no migration, delete the old database and create a new one (then SeedCallback
            // fills it). Do NOT also call fallbackToDestructiveMigrationOnDowngrade() – it
            // turns "migration required" back on and causes "A migration from X to Y was required".
            return builder
                .addCallback(SeedCallback())
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}

/**
 * Fills the database with SeedData.kt.
 *
 * Order of Room's steps (important!):
 *   new database:      create tables -> onCreate() -> onOpen()
 *   version changed:   drop tables -> onDestructiveMigration() -> create tables -> onOpen()
 * In onDestructiveMigration() the table does NOT exist yet, so we only set a flag
 * there and insert the seed data in onOpen(), when the table is ready.
 */
private class SeedCallback : RoomDatabase.Callback() {

    @Volatile
    private var seedOnOpen = false

    override fun onCreate(db: SupportSQLiteDatabase) {
        DebugLog.ok("DATABASE", "Room created table '${EventEntity.TABLE}' (version ${EventDatabase.DB_VERSION})")
        insertSeed(db)
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        DebugLog.info("DATABASE", "Database version changed -> recreating, SeedData will be inserted on open")
        seedOnOpen = true
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        if (seedOnOpen) {
            seedOnOpen = false
            insertSeed(db)
        }
    }

    private fun insertSeed(db: SupportSQLiteDatabase) {
        SeedData.ENTRIES.forEach { entry ->
            db.insert(EventEntity.TABLE, SQLiteDatabase.CONFLICT_ABORT, entry.toContentValues())
        }
        DebugLog.ok("DATABASE", "Inserted ${SeedData.ENTRIES.size} manual entries from SeedData.kt")
    }
}
