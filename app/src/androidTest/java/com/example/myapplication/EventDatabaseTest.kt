package com.example.myapplication

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room tests – run on a device/emulator.
 * Uses an IN-MEMORY database, so the real events.db is never touched.
 */
@RunWith(AndroidJUnit4::class)
class EventDatabaseTest {

    private lateinit var db: EventDatabase
    private lateinit var dao: EventDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = EventDatabase.build(context, name = null)
        dao = db.eventDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun newDatabase_containsSeedData() = runBlocking<Unit> {
        assertEquals(SeedData.ENTRIES.size.toLong(), dao.count())
    }

    @Test
    fun insert_thenRead() = runBlocking<Unit> {
        val id = dao.insert(EventEntity(title = "Test event", date = "2026-12-24", time = "18:00"))
        val stored = dao.getById(id)
        assertNotNull(stored)
        assertEquals("Test event", stored!!.title)
        assertEquals("18:00", stored.time)
        assertEquals(false, stored.saved)
    }

    @Test
    fun markSaved_updatesStatus() = runBlocking<Unit> {
        val id = dao.insert(EventEntity(title = "Test event", date = "2026-12-24"))
        assertEquals(1, dao.markSaved(id, true))
        assertEquals(true, dao.getById(id)!!.saved)
    }

    @Test
    fun reseed_restoresSeedData() = runBlocking<Unit> {
        dao.insert(EventEntity(title = "Extra", date = "2026-12-24"))
        dao.reseed(SeedData.ENTRIES)
        assertEquals(SeedData.ENTRIES.size.toLong(), dao.count())
    }

    /**
     * Regression test: an old database file (version 1, hand-written SQLite from the
     * first prototype) must be replaced by the Room database + SeedData, not crash with
     * "A migration from 1 to 2 was required but not found".
     */
    @Test
    fun oldVersion1Database_isRecreatedWithSeedData() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "events_upgrade_test.db"
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { old ->
            old.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL)")
            old.execSQL("INSERT INTO events (title) VALUES ('old row')")
            old.version = 1
        }

        val upgraded = EventDatabase.build(context, name)
        try {
            assertEquals(SeedData.ENTRIES.size.toLong(), upgraded.eventDao().count())
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun observeAll_emitsNewRows() = runBlocking<Unit> {
        dao.insert(EventEntity(title = "Live", date = "2026-12-24"))
        val list = dao.observeAll().first()
        assertTrue(list.any { it.title == "Live" })
    }
}
