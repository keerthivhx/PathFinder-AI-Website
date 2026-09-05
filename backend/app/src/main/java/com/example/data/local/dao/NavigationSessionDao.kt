package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.NavigationEventEntity
import com.example.data.local.entity.NavigationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NavigationSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: NavigationSessionEntity)

    @Update
    suspend fun updateSession(session: NavigationSessionEntity)

    @Query("SELECT * FROM navigation_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): NavigationSessionEntity?

    @Query("SELECT * FROM navigation_sessions WHERE status IN ('NAVIGATING', 'STARTED', 'PAUSED') ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getActiveSession(): NavigationSessionEntity?

    @Query("SELECT * FROM navigation_sessions ORDER BY startTimestamp DESC")
    fun getAllSessionsFlow(): Flow<List<NavigationSessionEntity>>

    @Query("SELECT * FROM navigation_sessions ORDER BY startTimestamp DESC")
    suspend fun getAllSessions(): List<NavigationSessionEntity>

    @Query("SELECT * FROM navigation_sessions WHERE buildingId = :buildingId ORDER BY startTimestamp DESC")
    fun getSessionsForBuildingFlow(buildingId: String): Flow<List<NavigationSessionEntity>>

    @Query("SELECT * FROM navigation_sessions WHERE buildingId = :buildingId ORDER BY startTimestamp DESC")
    suspend fun getSessionsForBuilding(buildingId: String): List<NavigationSessionEntity>

    @Query("UPDATE navigation_sessions SET status = 'ABANDONED', navigationResult = 'ABANDONED', endTimestamp = :timestamp WHERE status IN ('NAVIGATING', 'STARTED', 'PAUSED')")
    suspend fun markOrphanedSessionsAbandoned(timestamp: Long)

    @Query("DELETE FROM navigation_sessions WHERE isDemoSession = 1")
    suspend fun deleteDemoSessions()

    @Query("SELECT * FROM navigation_sessions WHERE isSynced = 0")
    suspend fun getPendingSyncSessions(): List<NavigationSessionEntity>

    @Query("UPDATE navigation_sessions SET isSynced = 1 WHERE sessionId IN (:sessionIds)")
    suspend fun markSessionsSynced(sessionIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: NavigationEventEntity)

    @Query("SELECT * FROM navigation_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEventsForSession(sessionId: String): Flow<List<NavigationEventEntity>>

    @Query("SELECT * FROM navigation_events WHERE buildingId = :buildingId AND timestamp >= :fromTimestamp ORDER BY timestamp DESC")
    suspend fun getEventsForBuilding(buildingId: String, fromTimestamp: Long): List<NavigationEventEntity>

    @Query("DELETE FROM navigation_events WHERE isDemo = 1")
    suspend fun deleteDemoEvents()
}
