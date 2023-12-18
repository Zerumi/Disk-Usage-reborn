/*
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2008-2011 Ivan Volosyuk
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
package io.github.diskusagereborn.core.scanner

import android.annotation.SuppressLint
import android.system.ErrnoException
import android.system.Os
import io.github.diskusagereborn.core.data.source.LegacyFile
import io.github.diskusagereborn.core.fs.entity.FileSystemEntrySmall
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry
import io.github.diskusagereborn.core.fs.entity.FileSystemFile
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER
import java.io.IOException
import java.util.Arrays
import java.util.PriorityQueue

class Scanner(
    private val maxDepth: Int,
    private val blockSize: Long,
    allocatedBlocks: Long,
    private val maxHeapSize: Int
) {
    private val blockSizeIn512Bytes: Long = blockSize / 512
    private val sizeThreshold: Long
    private var createdNode: FileSystemEntry? = null
    private var createdNodeSize = 0
    private var createdNodeNumFiles = 0
    private var createdNodeNumDirs = 0
    private var heapSize = 0
    private val smallLists = PriorityQueue<SmallList>()
    private var pos: Long = 0
    private var lastCreatedFile: FileSystemEntry? = null
    private var dev: Long = 0

    private class SmallList(
        var parent: FileSystemEntry?,
        var children: Array<FileSystemEntry?>,
        var heapSize: Int,
        blocks: Long
    ) : Comparable<SmallList?> {
        var spaceEfficiency: Float

        init {
            spaceEfficiency = blocks / heapSize.toFloat()
        }

        override operator fun compareTo(other: SmallList?): Int {
            if (other != null) {
                return spaceEfficiency.compareTo(other.spaceEfficiency)
            }
            throw NullPointerException()
        }
    }

    init {
        sizeThreshold = (allocatedBlocks shl FileSystemEntry.blockOffset) / (maxHeapSize / 2)
        //    this.blockAllowance = (allocatedBlocks << FileSystemEntry.blockOffset) / 2;
//    this.blockAllowance = (maxHeap / 2) * sizeThreshold;
        LOGGER.d("Scanner: allocatedBlocks %s", allocatedBlocks)
        LOGGER.d("Scanner: maxHeap %s", maxHeapSize)
        LOGGER.d(
            "Scanner: sizeThreshold = %s",
            sizeThreshold / (1 shl FileSystemEntry.blockOffset).toFloat()
        )
    }

    @Throws(IOException::class)
    fun scan(file: LegacyFile): FileSystemEntry? {
        val stBlocks: Long
        try {
            val stat = Os.stat(file.canonicalPath)
            dev = stat.st_dev
            stBlocks = stat.st_blocks
        } catch (e: ErrnoException) {
            throw IOException("Failed to find root folder", e)
        }
        scanDirectory(null, file, 0, stBlocks / blockSizeIn512Bytes)
        var extraHeap = 0

        // Restoring blocks
        for (list: SmallList in smallLists) {
//      print("restored", list);
            val oldChildren = list.parent!!.children ?: continue
            val addChildren = list.children
            val newChildren = arrayOfNulls<FileSystemEntry>(oldChildren.size - 1 + addChildren.size)
            System.arraycopy(addChildren, 0, newChildren, 0, addChildren.size)
            run {
                var pos: Int = addChildren.size
                var i = 0
                while (i < oldChildren.size) {
                    val c: FileSystemEntry? = oldChildren[i]
                    if (c !is FileSystemEntrySmall) {
                        newChildren[pos++] = c
                    }
                    i++
                }
            }
            Arrays.sort(newChildren, FileSystemEntry.COMPARE)
            list.parent!!.children = newChildren
            extraHeap += list.heapSize
        }
        LOGGER.d("allocated $extraHeap B of extra heap")
        LOGGER.d("allocated " + (extraHeap + createdNodeSize) + " B total")
        return createdNode
    }

    /**
     * Scan directory object.
     * This constructor starts recursive scan to find all descendent files and directories.
     * Stores parent into field, name obtained from file, size of this directory
     * is calculated as a sum of all children.
     * @param parent parent directory object.
     * @param file corresponding File object
     * @param depth current directory tree depth
     */
    @SuppressLint("DefaultLocale")
    private fun scanDirectory(
        parent: FileSystemEntry?,
        file: LegacyFile,
        depth: Int,
        selfBlocks: Long
    ) {
        val name = file.name
        if (name != null) {
            makeNode(parent, name)
        }
        createdNodeNumDirs = 1
        createdNodeNumFiles = 0
        if (depth == maxDepth) {
            createdNode!!.setSizeInBlocks(calculateSize(file), blockSize)
            return
        }
        var listNames: Array<String?>? = null
        try {
            listNames = file.list()
        } catch (io: SecurityException) {
            LOGGER.d("list files", io)
        }
        if (listNames == null) return
        val thisNode = createdNode
        var thisNodeSize = createdNodeSize
        var thisNodeNumDirs = 1
        var thisNodeNumFiles = 0
        var thisNodeSizeSmall = 0
        var thisNodeNumFilesSmall = 0
        var thisNodeNumDirsSmall = 0
        var smallBlocks: Long = 0
        val children = ArrayList<FileSystemEntry?>()
        val smallChildren = ArrayList<FileSystemEntry?>()
        var blocks = selfBlocks
        for (listName: String? in listNames) {
            val childFile = file.getChild(listName)

//      if (isLink(child)) continue;
//      if (isSpecial(child)) continue;
            var stBlocks: Long
            var stSize: Long
            try {
                val res = Os.stat(childFile?.canonicalPath)
                // Not regular file and not folder
//        if ((res.st_mode & 0x0100000) == 0 && (res.st_mode & 0x0040000) == 0) continue;
                stBlocks = res.st_blocks
                stSize = res.st_size
            } catch (e: ErrnoException) {
                continue
            } catch (e: IOException) {
                continue
            }
            var dirs = 0
            var files = 1
            if (childFile != null) {
                if (childFile.isFile) {
                    childFile.name?.let { makeNode(thisNode, it) }
                    createdNode!!.initSizeInBytesAndBlocks(
                        stSize,
                        stBlocks / blockSizeIn512Bytes
                    )
                    pos += createdNode!!.sizeInBlocks
                    lastCreatedFile = createdNode
                } else {
                    // directory
                    scanDirectory(thisNode, childFile, depth + 1, stBlocks / blockSizeIn512Bytes)
                    dirs = createdNodeNumDirs
                    files = createdNodeNumFiles
                }
            }
            val createdNodeBlocks = createdNode!!.sizeInBlocks
            blocks += createdNodeBlocks
            if (createdNodeSize * sizeThreshold > createdNode!!.encodedSize) {
                smallChildren.add(createdNode)
                thisNodeSizeSmall += createdNodeSize
                thisNodeNumFilesSmall += files
                thisNodeNumDirsSmall += dirs
                smallBlocks += createdNodeBlocks
            } else {
                children.add(createdNode)
                thisNodeSize += createdNodeSize
                thisNodeNumFiles += files
                thisNodeNumDirs += dirs
            }
        }
        thisNode!!.setSizeInBlocks(blocks, blockSize)
        thisNodeNumDirs += thisNodeNumDirsSmall
        thisNodeNumFiles += thisNodeNumFilesSmall
        var smallFilesEntry: FileSystemEntry? = null
        if ((thisNodeSizeSmall + thisNodeSize) * sizeThreshold <= thisNode.encodedSize
            || smallChildren.isEmpty()
        ) {
            children.addAll(smallChildren)
            thisNodeSize += thisNodeSizeSmall
        } else {
            val msg: String = if (thisNodeNumDirsSmall == 0) {
                String.format("<%d files>", thisNodeNumFilesSmall)
            } else if (thisNodeNumFilesSmall == 0) {
                String.format("<%d dirs>", thisNodeNumDirsSmall)
            } else {
                String.format(
                    "<%d dirs and %d files>",
                    thisNodeNumDirsSmall, thisNodeNumFilesSmall
                )
            }

//        String hidden_path = msg;
//        !! this is debug
//        for(FileSystemEntry p = thisNode; p != null; p = p.parent) {
//          hidden_path = p.name + "/" + hidden_path;
//        }
//        Log.d("diskusage", hidden_path + " = " + thisNodeSizeSmall);
            makeNode(thisNode, msg)
            // create another one with right type
            createdNode = FileSystemEntrySmall.makeNode(
                thisNode, msg,
                thisNodeNumFilesSmall + thisNodeNumDirsSmall
            )
            (createdNode as FileSystemEntrySmall?)?.setSizeInBlocks(smallBlocks, blockSize)
            smallFilesEntry = createdNode
            children.add(createdNode)
            thisNodeSize += createdNodeSize
            val list = SmallList(
                thisNode,
                smallChildren.toTypedArray<FileSystemEntry?>(),
                thisNodeSizeSmall,
                smallBlocks
            )
            smallLists.add(list)
        }

        // Magic to sort children and keep small files last in the array.
        if (children.size != 0) {
            var smallFilesEntrySize: Long = 0
            if (smallFilesEntry != null) {
                smallFilesEntrySize = smallFilesEntry.encodedSize
                smallFilesEntry.encodedSize = -1
            }
            thisNode.children = children.toTypedArray<FileSystemEntry?>()
            thisNode.children?.let { Arrays.sort(it, FileSystemEntry.COMPARE) }
            if (smallFilesEntry != null) {
                smallFilesEntry.encodedSize = smallFilesEntrySize
            }
        }
        createdNode = thisNode
        createdNodeSize = thisNodeSize
        createdNodeNumDirs = thisNodeNumDirs
        createdNodeNumFiles = thisNodeNumFiles
    }

    private fun makeNode(parent: FileSystemEntry?, name: String) {
        createdNode = FileSystemFile.makeNode(parent, name)
        createdNodeSize = (4 /* ref in FileSystemEntry[] */
                + 16 /* FileSystemEntry */ //      + 10000 /* dummy in FileSystemEntry */
                + 8 + 10 /* aproximation of size string */
                + 8 /* name header */
                + name.length * 2) /* name length */
        heapSize += createdNodeSize
        while (heapSize > maxHeapSize && !smallLists.isEmpty()) {
            val removed = smallLists.remove()
            heapSize -= removed.heapSize
            //      print("killed", removed);
        }
    }

    /**
     * Calculate size of the entry reading directory tree
     * @param file is file corresponding to this entry
     * @return size of entry in blocks
     */
    private fun calculateSize(file: LegacyFile): Long {
        if (file.isLink) return 0
        if (file.isFile) {
            return try {
                val res = Os.stat(file.canonicalPath)
                res.st_blocks
            } catch (e: ErrnoException) {
                0
            } catch (e: IOException) {
                0
            }
        }
        var list: Array<LegacyFile?>? = null
        try {
            list = file.listFiles()
        } catch (io: SecurityException) {
            LOGGER.e("Scanner.calculateSize(): list files", io)
        }
        if (list == null) return 0
        var size: Long = 1
        for (legacyFile: LegacyFile? in list) {
            if (legacyFile != null)
                size += calculateSize(legacyFile)
        }
        return size
    }
}
