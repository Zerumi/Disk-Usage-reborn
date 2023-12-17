package io.github.diskusagereborn.utils

import android.content.Context
import io.github.diskusagereborn.DiskUsageApplication

object AppHelper {

    @JvmStatic
    val appContext: Context
        get() = DiskUsageApplication.getInstance().applicationContext
}