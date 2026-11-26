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

    @Query("SELECT url FROM channels WHERE sourceListName = :sourceName AND isFavorite = 1")
    suspend fun getFavoriteUrlsBySource(sourceName: String): List<String>

    @Transaction
    suspend fun replaceChannelsForSource(sourceName: String, channels: List<ChannelEntity>) {
        val favoriteUrls = getFavoriteUrlsBySource(sourceName).toHashSet()
        deleteChannelsBySource(sourceName)
        if (channels.isNotEmpty()) {
            val merged = channels.map { entity ->
                if (entity.url in favoriteUrls) entity.copy(isFavorite = true) else entity
            }
            insertChannels(merged)
        }
    }

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
        WHERE (:allLists = 1 OR sourceListName = :sourceName)
        AND (:allCategories = 1 OR category = :category)
        AND (:query = '' OR name LIKE '%' || :query || '%')
    """)
    fun getFilteredChannels(
        query: String,
        category: String,
        sourceName: String,
        allLists: Boolean,
        allCategories: Boolean
    ): Flow<List<ChannelEntity>>
}
