package io.github.diskusagereborn

import android.app.Application

class DiskUsageApplication : Application() {
    companion object {

        @Volatile
        private var instance : DiskUsageApplication? = null

        fun getInstance() =
            instance ?:
            throw IllegalStateException("DiskUsage application is not created!")
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
