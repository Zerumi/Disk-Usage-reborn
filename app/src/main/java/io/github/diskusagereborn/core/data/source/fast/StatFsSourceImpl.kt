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
package io.github.diskusagereborn.core.data.source.fast

import android.os.StatFs
import io.github.diskusagereborn.core.data.source.StatFsSource

class StatFsSourceImpl(path: String?) : StatFsSource {
    private val statFs: StatFs

    init {
        statFs = StatFs(path)
    }

    @Deprecated("")
    override val availableBlocks: Int
        get() = statFs.availableBlocks

    override val availableBlocksLong: Long
        get() = statFs.availableBlocksLong

    override val availableBytes: Long
        get() = statFs.availableBytes

    @Deprecated("")
    override val blockCount: Int
        get() = statFs.blockCount

    override val blockCountLong: Long
        get() = statFs.blockCountLong

    @Deprecated("")
    override val blockSize: Int
        get() = statFs.blockSize

    override val blockSizeLong: Long
        get() = statFs.blockSizeLong

    override val freeBytes: Long
        get() = statFs.freeBytes

    @Deprecated("")
    override val freeBlocks: Int
        get() = statFs.freeBlocks

    override val freeBlocksLong: Long
        get() = statFs.freeBlocksLong

    override val totalBytes: Long
        get() = statFs.totalBytes
}
