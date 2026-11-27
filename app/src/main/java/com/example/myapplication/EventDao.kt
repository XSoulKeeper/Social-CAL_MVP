package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * ROOM DAO (Data Access Object) = all operations on the `events` table.
 *
 * You only write the function signature + SQL; Room generates the code.
 * The SQL inside @Query is CHECKED WHILE BUILDING – a typo in a table or
 * column name is a compile error, not a crash on the phone.
 *
 * - `suspend fun`  -> runs on Room's background thread, call it from a coroutine
 * - `Flow<...>`    -> a live query: emits a new list every time the table changes
 *
 * New query = new function here, e.g.
 *   @Query("SELECT * FROM events WHERE saved = 0")
 *   suspend fun getNotSaved(): List<EventEntity>
 */
@Dao
abstract class EventDao {

    @Insert
    abstract suspend fun insert(event: EventEntity): Long

    @Insert
    abstract suspend fun insertAll(events: List<EventEntity>)

    @Query("SELECT * FROM events ORDER BY created_at DESC, id DESC")
    abstract suspend fun getAll(): List<EventEntity>

    /** Live list for the History screen – updates automatically after insert / markSaved. */
    @Query("SELECT * FROM events ORDER BY created_at DESC, id DESC")
    abstract fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    abstract suspend fun getById(id: Long): EventEntity?

    @Query("SELECT COUNT(*) FROM events")
    abstract suspend fun count(): Long

    /** @return number of updated rows (0 = no event with this id) */
    @Query("UPDATE events SET saved = :saved WHERE id = :id")
    abstract suspend fun markSaved(id: Long, saved: Boolean): Int

    @Query("DELETE FROM events")
    abstract suspend fun deleteAll()

    /** Delete everything and insert [seed] – both or nothing (one transaction). */
    @Transaction
    open suspend fun reseed(seed: List<EventEntity>) {
        deleteAll()
        insertAll(seed)
    }
}
