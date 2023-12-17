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
import io.github.diskusagereborn.core.data.source.fast.NativeScannerStream
import io.github.diskusagereborn.core.fs.entity.FileSystemEntrySmall
import io.github.diskusagereborn.core.fs.entity.FileSystemFile
import io.github.diskusagereborn.core.fs.mount.MountPoint
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER
import java.io.IOException
import java.io.InputStream
import java.util.Arrays
import java.util.PriorityQueue

class NativeScanner(
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

    // private volatile int deepDepth = 0;
    fun lastCreatedFile(): FileSystemEntry? {
        return lastCreatedFile
    }

    fun pos(): Long {
        return pos
    }

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

    private var `is`: InputStream? = null
    private var offset = 0
    private var allocated = 0
    private val buffer = ByteArray(bufsize)
    private fun move() {
//    Log.d("diskusage", "MOVE!");
        if (offset == 0) throw RuntimeException("Error: too large entity size")
        System.arraycopy(buffer, offset, buffer, 0, allocated - offset)
        allocated -= offset
        offset = 0
    }

    @Throws(IOException::class)
    fun read() {
//    Log.d("diskusage", "READ!");
        if (allocated == bufsize) {
            move()
        }
        val res = `is`!!.read(buffer, allocated, (bufsize - allocated).coerceAtMost(256))
        if (res <= 0) {
            throw RuntimeException("Error: no more data")
        }
        allocated += res
    }

    @get:Throws(IOException::class)
    val byte: Byte
        get() {
            while (true) {
                if (offset < allocated) {
                    return buffer[offset++]
                }
                read()
            }
        }

    @get:Throws(IOException::class)
    val long: Long
        get() {
            var res: Long = 0
            var b: Byte
            while (byte.also { b = it }.toInt() != 0) {
                if (b < '0'.code.toByte() || b > '9'.code.toByte()) throw RuntimeException("Error: number format error")
                res = res * 10 + (b - '0'.code.toByte())
            }
            //    Log.d("diskusage", "long = " + res);
            return res
        }

    @get:Throws(IOException::class)
    val string: String
        get() {
            val buffer = buffer
            var startPos = offset
            while (true) {
                for (i in startPos until allocated) {
                    if (buffer[i].toInt() == 0) {
                        val res = String(buffer, offset, i - offset, charset("UTF-8"))
                        offset = i + 1
                        //          Log.d("diskusage", "string = " + res);
                        return res
                    }
                }
                val startOffset = startPos - offset
                read()
                startPos = offset + startOffset
            }
        }

    enum class Type {
        NONE,
        DIR,
        FILE
    }

    @get:Throws(IOException::class)
    val type: Type
        get() {
            return when (byte.toInt()) {
                'D'.code -> Type.DIR
                'F'.code -> Type.FILE
                'Z'.code -> Type.NONE
                else -> throw RuntimeException("Error: incorrect entity type")
            }
        }

    init {
        sizeThreshold = (allocatedBlocks shl FileSystemEntry.blockOffset) / (maxHeapSize / 2)
        //    this.blockAllowance = (allocatedBlocks << FileSystemEntry.blockOffset) / 2;
//    this.blockAllowance = (maxHeap / 2) * sizeThreshold;
        LOGGER.d("NativeScanner: allocatedBlocks %s", allocatedBlocks)
        LOGGER.d("NativeScanner: maxHeap %s", maxHeapSize)
        LOGGER.d(
            "NativeScanner: sizeThreshold = %s",
            sizeThreshold / (1 shl FileSystemEntry.blockOffset).toFloat()
        )
    }

    private fun print(msg: String, list: SmallList) {
        val hiddenPath = StringBuilder()
        // FIXME: this is debug
        var p = list.parent
        while (p != null) {
            hiddenPath.insert(0, p.name + "/")
            p = p.parent
        }
        LOGGER.d("%s %s = %s %s", msg, hiddenPath, list.heapSize, list.spaceEfficiency)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun scan(mountPoint: MountPoint): FileSystemEntry? {
        `is` = mountPoint.root?.let { NativeScannerStream.Factory.create(it, mountPoint.isRootRequired) }
        // while (getByte() != 0);
        val type = type
        if (type != Type.DIR) throw RuntimeException("Error: no mount point")
        scanDirectory(null, string, 0)
        LOGGER.d("NativeScanner.scan(): Allocated %s B of heap", createdNodeSize)
        var extraHeap = 0

        // Restoring blocks
        for (list: SmallList in smallLists) {
            print("restored", list)
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
        if (offset != allocated) throw RuntimeException("Error: extra data, " + (allocated - offset) + " bytes")
        (`is` as NativeScannerStream).close()
        return createdNode
    }

    private class SoftStack {
        enum class State {
            PRE_LOOP,
            LOOP,
            POST_LOOP
        }

        var state: State? = null
        var parent: FileSystemEntry? = null
        var name: String? = null
        var depth = 0
        var dirBlockSize: Long = 0
        var thisNode: FileSystemEntry? = null
        var thisNodeSize = 0
        var thisNodeNumDirs = 0
        var thisNodeNumFiles = 0
        var thisNodeSizeSmall = 0
        var thisNodeNumFilesSmall = 0
        var thisNodeNumDirsSmall = 0
        var smallBlocks: Long = 0
        var children: ArrayList<FileSystemEntry?>? = null
        var smallChildren: ArrayList<FileSystemEntry?>? = null
        var blocks: Long = 0
        var childType: Type? = null
        var dirs = 0
        var files = 0
        var prev: SoftStack? = null
    }

    // Very complicated version of scanDirectory() which uses soft stack instead
    // of real one.
    @SuppressLint("DefaultLocale")
    @Throws(IOException::class)
    private fun scanDirectorySoftStack(
        parent: FileSystemEntry?,
        name: String,
        depth: Int
    ) {
        var s: SoftStack? = SoftStack()
        s!!.parent = parent
        s.name = name
        s.depth = depth
        s.state = SoftStack.State.PRE_LOOP
        restart@ while (true) {
            when (s!!.state) {
                SoftStack.State.PRE_LOOP -> {
                    // deepDepth = s.depth;
                    s.dirBlockSize = long / blockSizeIn512Bytes
                    /*long dirBytesSize =*/long // side-effects
                    makeNode(s.parent, s.name)
                    createdNodeNumDirs = 1
                    createdNodeNumFiles = 0
                    s.thisNode = createdNode
                    lastCreatedFile = createdNode
                    s.thisNodeSize = createdNodeSize
                    s.thisNodeNumDirs = 1
                    s.thisNodeNumFiles = 0
                    s.thisNodeSizeSmall = 0
                    s.thisNodeNumFilesSmall = 0
                    s.thisNodeNumDirsSmall = 0
                    s.smallBlocks = 0
                    s.children = ArrayList()
                    s.smallChildren = ArrayList()
                    s.blocks = 0
                    s.state = SoftStack.State.LOOP
                    while (true) {
                        s!!.childType = type
                        if (s.childType == Type.NONE) break

                        //if (isLink(child)) continue;
                        //if (isSpecial(child)) continue;
                        s.dirs = 0
                        s.files = 1
                        if (s.childType == Type.FILE) {
                            makeNode(s.thisNode, string)
                            val childBlocks = long / blockSizeIn512Bytes
                            val childBytes = long
                            if (childBlocks == 0L) continue
                            createdNode!!.initSizeInBytesAndBlocks(
                                childBytes,
                                childBlocks
                            )
                            pos += createdNode!!.sizeInBlocks
                            lastCreatedFile = createdNode
                            //Log.d("diskusage", createdNode.path2());
                        } else {
                            // directory
                            val newS = SoftStack()
                            newS.prev = s
                            newS.parent = s.thisNode
                            newS.name = string
                            newS.depth = s.depth + 1
                            newS.state = SoftStack.State.PRE_LOOP
                            s = newS
                            continue@restart
                        }
                        val createdNodeBlocks = createdNode!!.sizeInBlocks
                        s.blocks += createdNodeBlocks
                        if (createdNodeSize * sizeThreshold > createdNode!!.encodedSize) {
                            s.smallChildren!!.add(createdNode)
                            s.thisNodeSizeSmall += createdNodeSize
                            s.thisNodeNumFilesSmall += s.files
                            s.thisNodeNumDirsSmall += s.dirs
                            s.smallBlocks += createdNodeBlocks
                        } else {
                            s.children!!.add(createdNode)
                            s.thisNodeSize += createdNodeSize
                            s.thisNodeNumFiles += s.files
                            s.thisNodeNumDirs += s.dirs
                        }
                    }
                    s!!.state = SoftStack.State.POST_LOOP
                    s.thisNode!!.setSizeInBlocks(s.blocks + s.dirBlockSize, blockSize)
                    s.thisNodeNumDirs += s.thisNodeNumDirsSmall
                    s.thisNodeNumFiles += s.thisNodeNumFilesSmall
                    var smallFilesEntry: FileSystemEntry? = null
                    if ((s.thisNodeSizeSmall + s.thisNodeSize) * sizeThreshold <= s.thisNode!!.encodedSize
                        || s.smallChildren!!.isEmpty()
                    ) {
                        s.children!!.addAll(s.smallChildren!!)
                        s.thisNodeSize += s.thisNodeSizeSmall
                    } else {
                        val msg: String = if (s.thisNodeNumDirsSmall == 0) {
                            String.format("<%d files>", s.thisNodeNumFilesSmall)
                        } else if (s.thisNodeNumFilesSmall == 0) {
                            String.format("<%d dirs>", s.thisNodeNumDirsSmall)
                        } else {
                            String.format(
                                "<%d dirs and %d files>",
                                s.thisNodeNumDirsSmall, s.thisNodeNumFilesSmall
                            )
                        }
                        makeNode(s.thisNode, msg)
                        // create another one with right type
                        createdNode = FileSystemEntrySmall.makeNode(
                            s.thisNode, msg,
                            s.thisNodeNumFilesSmall + s.thisNodeNumDirsSmall
                        )
                        (createdNode as FileSystemEntrySmall?)?.setSizeInBlocks(s.smallBlocks, blockSize)
                        smallFilesEntry = createdNode
                        s.children!!.add(createdNode)
                        s.thisNodeSize += createdNodeSize
                        val list = SmallList(
                            s.thisNode,
                            s.smallChildren!!.toTypedArray<FileSystemEntry?>(),
                            s.thisNodeSizeSmall,
                            s.smallBlocks
                        )
                        smallLists.add(list)
                    }

                    // Magic to sort children and keep small files last in the array.
                    if (s.children!!.size != 0) {
                        var smallFilesEntrySize: Long = 0
                        if (smallFilesEntry != null) {
                            smallFilesEntrySize = smallFilesEntry.encodedSize
                            smallFilesEntry.encodedSize = -1
                        }
                        s.thisNode!!.children = s.children!!.toTypedArray<FileSystemEntry?>()
                        Arrays.sort(s.thisNode!!.children, FileSystemEntry.COMPARE)
                        if (smallFilesEntry != null) {
                            smallFilesEntry.encodedSize = smallFilesEntrySize
                        }
                    }
                    createdNode = s.thisNode
                    createdNodeSize = s.thisNodeSize
                    createdNodeNumDirs = s.thisNodeNumDirs
                    createdNodeNumFiles = s.thisNodeNumFiles
                }

                SoftStack.State.LOOP -> {
                    s.state = SoftStack.State.LOOP
                    while (true) {
                        s!!.childType = type
                        if (s.childType == Type.NONE) break
                        s.dirs = 0
                        s.files = 1
                        if (s.childType == Type.FILE) {
                            makeNode(s.thisNode, string)
                            val childBlocks = long / blockSizeIn512Bytes
                            val childBytes = long
                            if (childBlocks == 0L) continue
                            createdNode!!.initSizeInBytesAndBlocks(
                                childBytes,
                                childBlocks
                            )
                            pos += createdNode!!.sizeInBlocks
                            lastCreatedFile = createdNode
                        } else {
                            val newS = SoftStack()
                            newS.prev = s
                            newS.parent = s.thisNode
                            newS.name = string
                            newS.depth = s.depth + 1
                            newS.state = SoftStack.State.PRE_LOOP
                            s = newS
                            continue@restart
                        }
                        val createdNodeBlocks = createdNode!!.sizeInBlocks
                        s.blocks += createdNodeBlocks
                        if (createdNodeSize * sizeThreshold > createdNode!!.encodedSize) {
                            s.smallChildren!!.add(createdNode)
                            s.thisNodeSizeSmall += createdNodeSize
                            s.thisNodeNumFilesSmall += s.files
                            s.thisNodeNumDirsSmall += s.dirs
                            s.smallBlocks += createdNodeBlocks
                        } else {
                            s.children!!.add(createdNode)
                            s.thisNodeSize += createdNodeSize
                            s.thisNodeNumFiles += s.files
                            s.thisNodeNumDirs += s.dirs
                        }
                    }
                    s!!.state = SoftStack.State.POST_LOOP
                    s.thisNode!!.setSizeInBlocks(s.blocks + s.dirBlockSize, blockSize)
                    s.thisNodeNumDirs += s.thisNodeNumDirsSmall
                    s.thisNodeNumFiles += s.thisNodeNumFilesSmall
                    var smallFilesEntry: FileSystemEntry? = null
                    if ((s.thisNodeSizeSmall + s.thisNodeSize) * sizeThreshold <= s.thisNode!!.encodedSize
                        || s.smallChildren!!.isEmpty()
                    ) {
                        s.children!!.addAll(s.smallChildren!!)
                        s.thisNodeSize += s.thisNodeSizeSmall
                    } else {
                        val msg: String = if (s.thisNodeNumDirsSmall == 0) {
                            String.format("<%d files>", s.thisNodeNumFilesSmall)
                        } else if (s.thisNodeNumFilesSmall == 0) {
                            String.format("<%d dirs>", s.thisNodeNumDirsSmall)
                        } else {
                            String.format(
                                "<%d dirs and %d files>",
                                s.thisNodeNumDirsSmall, s.thisNodeNumFilesSmall
                            )
                        }
                        makeNode(s.thisNode, msg)
                        createdNode = FileSystemEntrySmall.makeNode(
                            s.thisNode, msg,
                            s.thisNodeNumFilesSmall + s.thisNodeNumDirsSmall
                        )
                        (createdNode as FileSystemEntrySmall?)?.setSizeInBlocks(s.smallBlocks, blockSize)
                        smallFilesEntry = createdNode
                        s.children!!.add(createdNode)
                        s.thisNodeSize += createdNodeSize
                        val list = SmallList(
                            s.thisNode,
                            s.smallChildren!!.toTypedArray<FileSystemEntry?>(),
                            s.thisNodeSizeSmall,
                            s.smallBlocks
                        )
                        smallLists.add(list)
                    }
                    if (s.children!!.size != 0) {
                        var smallFilesEntrySize: Long = 0
                        if (smallFilesEntry != null) {
                            smallFilesEntrySize = smallFilesEntry.encodedSize
                            smallFilesEntry.encodedSize = -1
                        }
                        s.thisNode!!.children = s.children!!.toTypedArray<FileSystemEntry?>()
                        Arrays.sort(s.thisNode!!.children, FileSystemEntry.COMPARE)
                        if (smallFilesEntry != null) {
                            smallFilesEntry.encodedSize = smallFilesEntrySize
                        }
                    }
                    createdNode = s.thisNode
                    createdNodeSize = s.thisNodeSize
                    createdNodeNumDirs = s.thisNodeNumDirs
                    createdNodeNumFiles = s.thisNodeNumFiles
                }

                SoftStack.State.POST_LOOP -> {
                    s.state = SoftStack.State.POST_LOOP
                    s.thisNode!!.setSizeInBlocks(s.blocks + s.dirBlockSize, blockSize)
                    s.thisNodeNumDirs += s.thisNodeNumDirsSmall
                    s.thisNodeNumFiles += s.thisNodeNumFilesSmall
                    var smallFilesEntry: FileSystemEntry? = null
                    if ((s.thisNodeSizeSmall + s.thisNodeSize) * sizeThreshold <= s.thisNode!!.encodedSize
                        || s.smallChildren!!.isEmpty()
                    ) {
                        s.children!!.addAll(s.smallChildren!!)
                        s.thisNodeSize += s.thisNodeSizeSmall
                    } else {
                        val msg: String = if (s.thisNodeNumDirsSmall == 0) {
                            String.format("<%d files>", s.thisNodeNumFilesSmall)
                        } else if (s.thisNodeNumFilesSmall == 0) {
                            String.format("<%d dirs>", s.thisNodeNumDirsSmall)
                        } else {
                            String.format(
                                "<%d dirs and %d files>",
                                s.thisNodeNumDirsSmall, s.thisNodeNumFilesSmall
                            )
                        }
                        makeNode(s.thisNode, msg)
                        createdNode = FileSystemEntrySmall.makeNode(
                            s.thisNode, msg,
                            s.thisNodeNumFilesSmall + s.thisNodeNumDirsSmall
                        )
                        (createdNode as FileSystemEntrySmall?)?.setSizeInBlocks(s.smallBlocks, blockSize)
                        smallFilesEntry = createdNode
                        s.children!!.add(createdNode)
                        s.thisNodeSize += createdNodeSize
                        val list = SmallList(
                            s.thisNode,
                            s.smallChildren!!.toTypedArray<FileSystemEntry?>(),
                            s.thisNodeSizeSmall,
                            s.smallBlocks
                        )
                        smallLists.add(list)
                    }
                    if (s.children!!.size != 0) {
                        var smallFilesEntrySize: Long = 0
                        if (smallFilesEntry != null) {
                            smallFilesEntrySize = smallFilesEntry.encodedSize
                            smallFilesEntry.encodedSize = -1
                        }
                        s.thisNode!!.children = s.children!!.toTypedArray<FileSystemEntry?>()
                        Arrays.sort(s.thisNode!!.children, FileSystemEntry.COMPARE)
                        if (smallFilesEntry != null) {
                            smallFilesEntry.encodedSize = smallFilesEntrySize
                        }
                    }
                    createdNode = s.thisNode
                    createdNodeSize = s.thisNodeSize
                    createdNodeNumDirs = s.thisNodeNumDirs
                    createdNodeNumFiles = s.thisNodeNumFiles
                }

                else -> {}
            }
            s = s.prev
            if (s == null) return
            s.dirs = createdNodeNumDirs
            s.files = createdNodeNumFiles
            // Finish missed part of inner loop
            val createdNodeBlocks = createdNode!!.sizeInBlocks
            s.blocks += createdNodeBlocks
            if (createdNodeSize * sizeThreshold > createdNode!!.encodedSize) {
                s.smallChildren!!.add(createdNode)
                s.thisNodeSizeSmall += createdNodeSize
                s.thisNodeNumFilesSmall += s.files
                s.thisNodeNumDirsSmall += s.dirs
                s.smallBlocks += createdNodeBlocks
            } else {
                s.children!!.add(createdNode)
                s.thisNodeSize += createdNodeSize
                s.thisNodeNumFiles += s.files
                s.thisNodeNumDirs += s.dirs
            }
        }
    }

    /**
     * Scan directory object.
     * This constructor starts recursive scan to find all descendent files and directories.
     * Stores parent into field, name obtained from file, size of this directory
     * is calculated as a sum of all children.
     * @param parent parent directory object.
     * @param depth current directory tree depth
     * @throws IOException if can not read directory or else
     */
    @SuppressLint("DefaultLocale")
    @Throws(IOException::class)
    private fun scanDirectory(
        parent: FileSystemEntry?, name: String,
        depth: Int
    ) {
        if (depth > 10) {
            scanDirectorySoftStack(parent, name, depth)
            return
        }
        val dirBlockSize = long / blockSizeIn512Bytes
        /*long dirBytesSize =*/long
        makeNode(parent, name)
        createdNodeNumDirs = 1
        createdNodeNumFiles = 0
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
        var blocks: Long = 0
        while (true) {
            val childType = type
            if (childType == Type.NONE) break

//      if (isLink(child)) continue;
//      if (isSpecial(child)) continue;
            var dirs = 0
            var files = 1
            if (childType == Type.FILE) {
                makeNode(thisNode, string)
                val childBlocks = long / blockSizeIn512Bytes
                val childBytes = long
                if (childBlocks == 0L) continue
                createdNode!!.initSizeInBytesAndBlocks(childBytes, childBlocks)
                pos += createdNode!!.sizeInBlocks
                lastCreatedFile = createdNode
                //        Log.d("diskusage", createdNode.path2());
            } else {
                // directory
                scanDirectory(thisNode, string, depth + 1)
                dirs = createdNodeNumDirs
                files = createdNodeNumFiles
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
        thisNode!!.setSizeInBlocks(blocks + dirBlockSize, blockSize)
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
//        // FIXME: this is debug
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
            Arrays.sort(thisNode.children, FileSystemEntry.COMPARE)
            if (smallFilesEntry != null) {
                smallFilesEntry.encodedSize = smallFilesEntrySize
            }
        }
        createdNode = thisNode
        createdNodeSize = thisNodeSize
        createdNodeNumDirs = thisNodeNumDirs
        createdNodeNumFiles = thisNodeNumFiles
    }

    private fun makeNode(parent: FileSystemEntry?, name: String?) {
//    try {
//      Thread.sleep(10);
//    } catch (Throwable t) {}
        createdNode = name?.let { FileSystemFile.makeNode(parent, it) }
        createdNodeSize = (4 /* ref in FileSystemEntry[] */
                + 16 /* FileSystemEntry */ //      + 10000 /* dummy in FileSystemEntry */
                + 8 + 10 /* aproximation of size string */
                + 8 /* name header */
                + (name!!.length * 2)) /* name length */
        heapSize += createdNodeSize
        while (heapSize > maxHeapSize && !smallLists.isEmpty()) {
            val removed = smallLists.remove()
            heapSize -= removed.heapSize
            print("killed", removed)
        }
    }

    companion object {
        private const val bufsize = 65536
    }
}
