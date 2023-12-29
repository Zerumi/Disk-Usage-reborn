/*
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2008 Ivan Volosyuk
 *
 * DiskUsage Reborn - Rewritten on modern stack version of DiskUsage
 * Copyright (C) 2023 Zerumi
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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