package com.example.xuper

import android.app.Application
import android.database.CursorWindow
import com.example.xuper.data.AppDatabase
import com.example.xuper.data.ChannelRepository
import java.lang.reflect.Field

class XuperApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ChannelRepository(database.channelDao()) }

    override fun onCreate() {
        super.onCreate()
        try {
            val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 20 * 1024 * 1024) // 20MB
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
