package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TremorEventDao {
    @Query("SELECT * FROM tremor_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<TremorEventEntity>>

    @Query("SELECT * FROM tremor_events WHERE severity = :severity ORDER BY timestamp DESC")
    fun getEventsBySeverity(severity: String): Flow<List<TremorEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TremorEventEntity): Long

    @Update
    suspend fun updateEvent(event: TremorEventEntity)

    @Query("DELETE FROM tremor_events")
    suspend fun clearAllEvents()

    @Query("SELECT COUNT(*) FROM tremor_events")
    fun getEventCount(): Flow<Int>
}
