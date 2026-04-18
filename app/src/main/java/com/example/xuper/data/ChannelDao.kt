package com.example.xuper.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean)

    @Query("DELETE FROM channels WHERE sourceListName = :sourceName")
    suspend fun deleteChannelsBySource(sourceName: String)

    @Query("DELETE FROM channels")
    suspend fun deleteAll()

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%'")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("""
        SELECT * FROM channels 
        WHERE (:sourceName = 'Todas las listas' OR sourceListName = :sourceName)
        AND (:category = 'Todos' OR category = :category)
        AND (name LIKE '%' || :query || '%')
    """)
    fun getFilteredChannels(query: String, category: String, sourceName: String): Flow<List<ChannelEntity>>
}
