package com.example.xuper.data

import com.example.xuper.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChannelRepository(private val channelDao: ChannelDao) {

    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels().map { entities ->
        entities.map { it.toModel() }
    }

    val favoriteChannels: Flow<List<Channel>> = channelDao.getFavoriteChannels().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun refreshChannels(channels: List<Channel>, sourceName: String) {
        channelDao.deleteChannelsBySource(sourceName)
        channelDao.insertChannels(channels.map { it.toEntity(sourceName) })
    }

    suspend fun toggleFavorite(channel: Channel, isFavorite: Boolean) {
        channelDao.updateFavoriteStatus(channel.url, isFavorite)
    }

    fun searchChannels(query: String): Flow<List<Channel>> = channelDao.searchChannels(query).map { entities ->
        entities.map { it.toModel() }
    }

    fun getFilteredChannels(query: String, category: String, sourceName: String): Flow<List<Channel>> {
        return channelDao.getFilteredChannels(query, category, sourceName).map { entities ->
            entities.map { it.toModel() }
        }
    }

    private fun ChannelEntity.toModel() = Channel(
        name = name,
        url = url,
        logo = logo,
        category = category,
        sourceListName = sourceListName,
        isFavorite = isFavorite
    )

    private fun Channel.toEntity(source: String) = ChannelEntity(
        name = name,
        url = url,
        logo = logo,
        category = category,
        sourceListName = source,
        isFavorite = isFavorite
    )
}
