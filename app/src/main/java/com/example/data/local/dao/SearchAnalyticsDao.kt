package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SearchAnalyticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchAnalyticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchAnalyticsEntity)

    @Query("SELECT * FROM search_analytics WHERE buildingId = :buildingId AND timestamp >= :fromTimestamp ORDER BY timestamp DESC")
    suspend fun getSearchesForBuilding(buildingId: String, fromTimestamp: Long): List<SearchAnalyticsEntity>

    @Query("SELECT * FROM search_analytics WHERE timestamp >= :fromTimestamp ORDER BY timestamp DESC")
    suspend fun getAllSearches(fromTimestamp: Long): List<SearchAnalyticsEntity>

    @Query("SELECT * FROM search_analytics WHERE buildingId = :buildingId AND isSuccessful = 0 AND timestamp >= :fromTimestamp ORDER BY timestamp DESC")
    suspend fun getZeroResultSearches(buildingId: String, fromTimestamp: Long): List<SearchAnalyticsEntity>

    @Query("SELECT * FROM search_analytics ORDER BY timestamp DESC")
    fun getAllSearchesFlow(): Flow<List<SearchAnalyticsEntity>>

    @Query("DELETE FROM search_analytics WHERE isDemo = 1")
    suspend fun deleteDemoSearches()
}
