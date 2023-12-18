package io.github.diskusagereborn.core.fs

import io.github.diskusagereborn.core.data.source.StatFsSource
import io.github.diskusagereborn.core.data.source.fast.StatFsSourceImpl
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry.Companion.calcSizeString
import io.github.diskusagereborn.core.fs.mount.MountPoint
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER

class FileSystemStats(mountPoint: MountPoint) {
    var blockSize: Long = 0
    var freeBlocks: Long = 0
    var busyBlocks: Long = 0
    var totalBlocks: Long = 0

    init {
        var stats: StatFsSource? = null
        try {
            stats = StatFsSourceImpl(mountPoint.root)
        } catch (e: IllegalArgumentException) {
            LOGGER.e(e, "Failed to get filesystem stats for " + mountPoint.root)
        }
        if (stats != null) {
            blockSize = stats.blockSizeLong
            freeBlocks = stats.availableBlocksLong
            totalBlocks = stats.blockCountLong
            busyBlocks = totalBlocks - freeBlocks
        } else {
            busyBlocks = 0
            totalBlocks = 0
            freeBlocks = 0
            blockSize = 512
        }
    }

    fun formatUsageInfo(): String {
        return if (totalBlocks == 0L) "Used <no information>" else String.format(
            "Used %s of %s",
            calcSizeString((busyBlocks * blockSize).toFloat()),
            calcSizeString((totalBlocks * blockSize).toFloat())
        )
    }
}