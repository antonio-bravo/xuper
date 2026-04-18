package com.example.xuper

import android.app.Application
import com.example.xuper.data.AppDatabase
import com.example.xuper.data.ChannelRepository

class XuperApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ChannelRepository(database.channelDao()) }
}
