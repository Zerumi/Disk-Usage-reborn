package io.github.diskusagereborn

import android.app.Application

class DiskUsageApplication : Application() {
    companion object {

        @Volatile
        private var instance : DiskUsageApplication? = null

        fun getInstance() =
            instance ?:
            throw IllegalStateException("DiskUsage application is not created!")

        const val RESULT_DELETE_CONFIRMED = 10
        const val RESULT_DELETE_CANCELED = 11
        const val STATE_KEY = "state"
        const val KEY_KEY = "key"
        const val DELETE_PATH_KEY = "path"
        const val DELETE_ABSOLUTE_PATH_KEY = "absolute_path"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
