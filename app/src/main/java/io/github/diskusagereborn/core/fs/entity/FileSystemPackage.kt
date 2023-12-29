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
package io.github.diskusagereborn.core.fs.entity

import android.content.pm.ApplicationInfo
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER
import java.util.Arrays

class FileSystemPackage(
    name: String?, pkg: String, codeSize: Long, dataSize: Long, cacheSize: Long, flags: Int
) : FileSystemEntry(null, name!!) {
    @JvmField
    val pkg: String
    private var codeSize: Long
    private var dataSize: Long
    private var cacheSize: Long
    private val flags: Int
    private val publicChildren = ArrayList<FileSystemRoot>()

    enum class ChildType {
        CODE,
        DATA,
        CACHE
    }

    init {
        var codeSizeInit = codeSize
        this.pkg = pkg
        this.cacheSize = cacheSize
        this.dataSize = dataSize - cacheSize
        this.flags = flags
        if (flags and ApplicationInfo.FLAG_SYSTEM != 0
            && flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
        ) {
            codeSizeInit = 0
        }

        // debug code
        if (flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0) {
            codeSizeInit = 0
        }
        this.codeSize = codeSizeInit
    }

    fun applyFilter(blockSize: Long) {
        //clearDrawingCache()
        var blocks: Long = 0
        val entries = ArrayList<FileSystemEntry>(publicChildren)
        entries.add(
            makeNode(null, "apk")
                .initSizeInBytes(codeSize, blockSize)
        )
        entries.add(
            makeNode(null, "data")
                .initSizeInBytes(dataSize, blockSize)
        )
        entries.add(
            makeNode(null, "cache")
                .initSizeInBytes(cacheSize, blockSize)
        )
        for (e in entries) {
            blocks += e.sizeInBlocks
        }
        setSizeInBlocks(blocks, blockSize)
        for (e in entries) {
            e.parent = this
        }
        children = entries.toArray(arrayOf())
        children?.let { Arrays.sort(it, COMPARE) }
    }

    override fun create(): FileSystemEntry {
        return FileSystemPackage(
            name, pkg, codeSize, dataSize, cacheSize,
            flags
        )
    }

    fun addPublicChild(child: FileSystemRoot, type: ChildType, blockSize: Long) {
        publicChildren.add(child)
        when (type) {
            ChildType.CODE -> {
                codeSize -= child.sizeInBlocks * blockSize
                if (codeSize < 0) {
                    LOGGER.d(
                        "FileSystemPackage.addPublicChild(): Code size negative %s for %s",
                        codeSize,
                        pkg
                    )
                    codeSize = 0
                }
            }

            ChildType.DATA -> {
                dataSize -= child.sizeInBlocks * blockSize
                if (dataSize < 0) {
                    LOGGER.d(
                        "FileSystemPackage.addPublicChild(): Data size negative %s for %s",
                        dataSize,
                        pkg
                    )
                    dataSize = 0
                }
            }

            ChildType.CACHE -> {
                cacheSize -= child.sizeInBlocks * blockSize
                if (cacheSize < 0) {
                    LOGGER.d(
                        "FileSystemPackage.addPublicChild(): Cache size negative %s for %s",
                        cacheSize,
                        pkg
                    )
                    cacheSize = 0
                }
            }
        }
    }
}
