package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FaultEventDao {
    @Query("SELECT * FROM fault_events ORDER BY timestamp DESC")
    fun getAllFaults(): Flow<List<FaultEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFault(fault: FaultEventEntity): Long

    @Query("UPDATE fault_events SET isCleared = 1 WHERE id = :faultId")
    suspend fun markFaultCleared(faultId: Long)

    @Query("DELETE FROM fault_events")
    suspend fun clearAll()
}
