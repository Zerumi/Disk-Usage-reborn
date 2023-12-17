package io.github.diskusagereborn.utils

import io.github.diskusagereborn.DiskUsageApplication
import java.io.File

object PathHelper {
    @JvmStatic
    fun getExternalAppFilesPaths(): Array<out File> {
        return DiskUsageApplication.getInstance()
            .getExternalFilesDirs(null)
    }
}