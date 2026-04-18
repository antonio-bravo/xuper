package com.example.xuper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val logo: String?,
    val category: String,
    val sourceListName: String,
    val isFavorite: Boolean = false
)
