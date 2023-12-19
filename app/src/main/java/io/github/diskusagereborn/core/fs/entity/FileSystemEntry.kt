/*
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2008 Ivan Volosyuk
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

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import io.github.diskusagereborn.R
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER
import java.util.Arrays
import java.util.Locale

open class FileSystemEntry protected constructor(@JvmField var parent: FileSystemEntry?, @JvmField var name: String) {
    // Object Fields:
    // The size suitable for painting without any operations (and sorting)
    // Bit layout:
    // 40 bits      | 24 bits
    // sizeInBlocks | reminder
    // reminder is encoded file size information suitable for formating sizeString.
    // reminder:
    // 3 bits         | 21 bits  (2**18 = 44,040,192)
    // sizeMultiplier | size in multiplier of bytes
    // sizeMultiplier:
    // 000 = multiplier=1,              format=(n_bytes "%d bytes", size)
    // 001 = multiplier=1024,           format=(n_kilobytes "%d KiB", size)
    // 010 = multiplier=1024,           format=(n_megabytes "%5.2f MiB", size / 1024.f)
    // 011 = multiplier=1024,           format=(n_megabytes10 "%5.1f MiB", size / 1024.f)
    // 100 = multiplier=1024*1024,      format=(n_megabytes100 "%d MiB", size)
    // 101 = multiplier=1024*1024,      format=(n_gigabytes "%5.2f GiB", size/ 1024.f)
    // 110 = multiplier=1024*1024,      format=(n_gigabytes10 "%5.1f GiB", size/ 1024.f)
    // 111 = multiplier=1024*1024*1024, format=(n_gigabytes100 "%d GiB", size)
    // Ranges for sizeMultipliers:
    // 0: sz < 1024:               "%4.0f bytes", sz
    // 1: sz < 1024 * 1024:        "%4.0f KiB", sz * (1f / 1024)
    // 2: sz < 1024 * 1024 * 10:   "%5.2f MiB", sz * (1f / 1024 / 1024)
    // 3: sz < 1024 * 1024 * 200:  "%5.1f MiB", sz * (1f / 1024 / 1024)
    // 4: sz >= 1024 * 1024 * 200: "%4.0f MiB", sz * (1f / 1024 / 1024)
    //
    @JvmField
    var encodedSize: Long = 0

    var children: Array<FileSystemEntry?>? = null

    val sizeInBlocks: Long
        get() = encodedSize shr blockOffset
    private val sizeForRendering: Long
        get() = encodedSize and blockMask.inv()

    private fun makeBytesPart(size: Long): Long {
        if (size < 1024) return size
        if (size < 1024 * 1024) return MULTIPLIER_KBYTES.toLong() or (size shr 10)
        if (size < 1024 * 1024 * 10) return MULTIPLIER_MBYTES.toLong() or (size shr 10)
        if (size < 1024 * 1024 * 200) return MULTIPLIER_MBYTES10.toLong() or (size shr 10)
        if (size < 1024L * 1024 * 1024) return MULTIPLIER_MBYTES100.toLong() or (size shr 20)
        if (size < 1024L * 1024 * 1024 * 10) return MULTIPLIER_GBYTES.toLong() or (size shr 20)
        return if (size < 1024L * 1024 * 1024 * 200) MULTIPLIER_GBYTES10.toLong() or (size shr 20) else MULTIPLIER_GBYTES100.toLong() or (size shr 30)
    }

    open val isDeletable: Boolean
        get() = false

    fun setSizeInBlocks(blocks: Long, blockSize: Long) {
        val bytes = blocks * blockSize
        encodedSize = blocks shl blockOffset or makeBytesPart(bytes)
    }

    fun initSizeInBytes(bytes: Long, blockSize: Long): FileSystemEntry {
        val blocks = (bytes + blockSize - 1) / blockSize
        encodedSize = blocks shl blockOffset or makeBytesPart(bytes)
        return this
    }

    fun initSizeInBytesAndBlocks(bytes: Long, blocks: Long): FileSystemEntry {
        encodedSize = blocks shl blockOffset or makeBytesPart(bytes)
        return this
    }

    fun setChildren(children: Array<FileSystemEntry?>, blockSize: Long): FileSystemEntry {
        this.children = children
        var blocks: Long = 0
        for (child in children) {
            blocks += child?.sizeInBlocks ?: 0
            child?.parent = this
        }
        setSizeInBlocks(blocks, blockSize)
        return this
    }

    class Compare : Comparator<FileSystemEntry?> {
        override fun compare(aa: FileSystemEntry?, bb: FileSystemEntry?): Int {
            if (aa?.encodedSize == bb?.encodedSize) {
                return 0
            }
            if (aa == null || bb == null) throw NullPointerException()
            return if (aa.encodedSize < bb.encodedSize) 1 else -1
        }
    }

    open fun create(): FileSystemEntry {
        return FileSystemEntry(null, name)
    }

    object SearchInterruptedException : RuntimeException()

    fun copy(): FileSystemEntry {
        if (Thread.interrupted()) throw SearchInterruptedException
        val copy = create()
        if (children != null) {
            val children = arrayOfNulls<FileSystemEntry>(children!!.size)
            for (i in this.children!!.indices) {
                children[i] = this.children!![i]!!.copy()
                val childCopy = children[i]
                childCopy!!.parent = copy
            }
            copy.children = children
        }
        copy.encodedSize = encodedSize
        return copy
    }

    fun filterChildren(pattern: CharSequence?, blockSize: Long): FileSystemEntry? {
//    res = Pattern.compile(Pattern.quote(pattern.toString()), Pattern.CASE_INSENSITIVE).matcher(name).find();
        if (children == null) return null
        val filteredChildren = ArrayList<FileSystemEntry>()
        for (child in children!!) {
            val childCopy = child!!.filter(pattern, blockSize)
            if (childCopy != null) {
                filteredChildren.add(childCopy)
            }
        }
        if (filteredChildren.size == 0) return null
        val children = arrayOfNulls<FileSystemEntry>(filteredChildren.size)
        filteredChildren.toArray(children)
        Arrays.sort(children, COMPARE)
        val copy = create()
        copy.children = children
        var size: Long = 0
        for (child in children) {
            size += child!!.sizeInBlocks
            child.parent = copy
        }
        copy.setSizeInBlocks(size, blockSize)
        return copy
    }

    open fun filter(pattern: CharSequence?, blockSize: Long): FileSystemEntry? {
        return if (name.lowercase(Locale.getDefault()).contains(pattern!!)) {
            copy()
        } else filterChildren(
            pattern,
            blockSize
        )
    }

    /**
     * Find index of directChild in 'children' field of this entry.
     * @return index of the directChild in 'children' field.
     */
    private fun getIndexOf(directChild: FileSystemEntry): Int {
        val children0 = children
        if (children0 != null) {
            for (child in children0) {
                if (child === directChild) {
                    return child.getIndexOf(directChild)
                }
            }
        }
        throw RuntimeException("something broken")
    }

    val next: FileSystemEntry?
        /**
         * Find entry which follows this entry in its the parent.
         * @return next entry in the same parent or this entry if there is no more entries
         */
        get() {
            val index = parent?.getIndexOf(this)
            if (index != null) {
                return if (index + 1 == parent?.children?.size) this else parent?.children?.get(index + 1)
            }
            return null
        }
    val prev: FileSystemEntry?
        /**
         * Find entry which precedes this entry in its the parent.
         * @return previous entry in the same parent or this entry if the entry is first
         */
        get() {
            val index = parent?.getIndexOf(this)
            if (index != null) {
                return if (index == 0) this else parent?.children?.get(index - 1)
            }
            return null
        }

    fun sizeString(): String {
        return calcSizeStringFromEncoded(encodedSize)
    }

    fun toTitleString(): String {
        val sizeString0 = sizeString()
        return if (children != null && children!!.isNotEmpty()) String.format(
            dir_name_size_num_dirs!!,
            name,
            sizeString0,
            children!!.size
        ) else if (sizeInBlocks == 0L) {
            String.format(dir_empty!!, name)
        } else {
            String.format(dir_name_size!!, name, sizeString0)
        }
    }

    fun path2(): String {
        val pathElements = ArrayList<String>()
        var current : FileSystemEntry? = this
        while (current != null) {
            pathElements.add(current.name)
            current = current.parent
        }
        pathElements.removeAt(pathElements.size - 1)
        pathElements.removeAt(pathElements.size - 1)
        val path = StringBuilder()
        var sep = ""
        for (i in pathElements.indices.reversed()) {
            path.append(sep)
            path.append(pathElements[i])
            sep = "/"
        }
        return path.toString()
    }

    private fun absolutePath(): String {
        return if (this is FileSystemRoot) {
            this.rootPath
        } else parent?.absolutePath() + "/" + name
    }

    /**
     * Find depth of 'entry' in current element.
     * @param sourceEntry file system entry
     * @return 1 for depth equal 1 and so on
     */
    fun depth(sourceEntry: FileSystemEntry): Int {
        var entry : FileSystemEntry? = sourceEntry
        var d = 0
        val root = this
        while (entry != null && entry !== root) {
            entry = entry.parent
            d++
        }
        return d
    }

    /**
     * Find and return entry on specified depth and offset in this entry used as root.
     * @param maxDepth maximum depth to find entry
     * @return nearest entry to the specified conditions
     */
    fun findEntry(maxDepth: Int, offset: Long): FileSystemEntry? {
        var currOffset: Long = 0
        var entry: FileSystemEntry? = this
        var children0 = children
        // Log.d("DiskUsage", "Starting entry search at " + entry.name);
        for (depth in 0 until maxDepth) {
            val nChildren = children0?.size ?: return null
            // Log.d("DiskUsage", "  Entry = " + entry.name);
            for (c in 0 until nChildren) {
                val e = children0?.get(c)
                val size = e!!.sizeForRendering
                if (currOffset + size < offset) {
                    currOffset += size
                    continue
                }

                // found entry
                entry = e
                children0 = e.children
                if (children0 == null) return entry
                else break
            }
        }
        return entry
    }

    /**
     * Returns offset in bytes (world coordinates) from start of this
     * object to the start of 'cursor' object.
     * @param sourceCursor file system entry cursor
     * @return offset in bytes
     */
    fun getOffset(sourceCursor: FileSystemEntry): Long {
        var cursor : FileSystemEntry? = sourceCursor
        var offset: Long = 0
        var dir: FileSystemEntry?
        val root = this

//    Log.d("diskusage", "getOffset()");
        while (cursor !== root) {
            dir = cursor?.parent
            val children = dir?.children
            if (children != null) {
                for (e in children) {
                    if (e === cursor) break
                    offset += e!!.sizeForRendering
                }
            }
            cursor = dir
        }
        return offset
    }

    fun remove(blockSize: Long) {
        val children0 = parent?.children
        val len = children0?.size ?: return
        for (i in 0 until len) {
            if (children0[i] !== this) continue

            // executed only once:
            parent?.children = arrayOfNulls(len - 1)
            System.arraycopy(children0, 0, parent?.children ?: return, 0, i)
            System.arraycopy(children0, i + 1, parent?.children ?: return, i, len - i - 1)
            //java.util.Arrays.sort(parent.children, this);
            var parent0 = parent
            val blocks = sizeInBlocks
            while (parent0 != null) {
                parent0.setSizeInBlocks(parent0.sizeInBlocks - blocks, blockSize)
                //parent0.clearDrawingCache()
                parent0.children?.let { Arrays.sort(it, COMPARE) }
                parent0 = parent0.parent
            }
            return
        }
    }

    fun insert(newEntry: FileSystemEntry, blockSize: Long) {
        val children0 = arrayOfNulls<FileSystemEntry>(children?.size?.plus(1) ?: return)
        children?.size?.let { children?.let { it1 -> System.arraycopy(it1, 0, children0, 0, it) } }
        children0[children?.size ?: return] = newEntry
        children = children0
        newEntry.parent = this
        var parent0 : FileSystemEntry? = this
        val blocks = newEntry.sizeInBlocks
        while (parent0 != null) {
            children?.let { Arrays.sort(it, COMPARE) }
            parent0.setSizeInBlocks(parent0.sizeInBlocks + blocks, blockSize)
            //parent0.clearDrawingCache()
            parent0 = parent0.parent
        }
    }

    /**
     * Walks through the path and finds the specified entry, null otherwise.
     * @param exactMatch is exact match
     */
    open fun getEntryByName(path: String, exactMatch: Boolean): FileSystemEntry? {
        LOGGER.d("FileSystemEntry.getEntryByName(): getEntryForName = %s", path)
        val pathElements = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var entry: FileSystemEntry? = this
        outer@ for (name in pathElements) {
            val children = entry!!.children ?: return null
            for (child in children) {
                entry = child
                if (name == entry!!.name) {
                    continue@outer
                }
            }
            return null
        }
        return entry
    }

    open val numFiles: Int
        /**
         * Just files, no directories.
         * @return count
         */
        get() {
            if (this is FileSystemEntrySmall) {
                val a : FileSystemEntrySmall = this
                return a.numFiles
            }
            if (children == null) return 1
            var numFiles = 0
            var hasFile = false
            for (entry in children!!) {
                if (entry!!.children == null) hasFile = true
                numFiles += entry.numFiles
            }
            if (hasFile) numFiles++
            return numFiles
        }

    companion object {
        private val bg = Paint()
        private val bg_emptySpace = Paint()
        private val cursor_fg = Paint()
        private val fg_rect = Paint()

        //  private static final Paint fg_rect = new Paint();
        private val fg2 = Paint()
        private val fill_bg = Paint()
        private val textPaintFolder = Paint()
        private val textPaintFile = Paint()
        private var n_bytes: String? = null
        private var n_kilobytes: String? = null
        private var n_megabytes: String? = null
        private var n_megabytes10: String? = null
        private var n_megabytes100: String? = null
        private var n_gigabytes: String? = null
        private var n_gigabytes10: String? = null
        private var n_gigabytes100: String? = null
        private var dir_name_size_num_dirs: String? = null
        private var dir_empty: String? = null
        private var dir_name_size: String? = null

        init {
            bg.color = Color.parseColor("#060118")
            bg_emptySpace.color = Color.parseColor("#063A43")
            bg.style = Paint.Style.FILL
            //    bg.setAlpha(255);
            fg_rect.color = Color.WHITE
            fg_rect.style = Paint.Style.STROKE
            fg_rect.flags = fg_rect.flags or Paint.ANTI_ALIAS_FLAG
            fg2.color = Color.parseColor("#18C5E7")
            fg2.style = Paint.Style.STROKE
            fg2.flags = fg2.flags or Paint.ANTI_ALIAS_FLAG
            fill_bg.color = Color.WHITE
            fill_bg.style = Paint.Style.FILL
            cursor_fg.color = Color.YELLOW
            cursor_fg.style = Paint.Style.STROKE
            textPaintFolder.color = Color.WHITE
            textPaintFolder.style = Paint.Style.FILL_AND_STROKE
            textPaintFolder.flags =
                textPaintFolder.flags or Paint.ANTI_ALIAS_FLAG
            textPaintFile.color = Color.parseColor("#18C5E7")
            textPaintFile.style = Paint.Style.FILL_AND_STROKE
            textPaintFile.flags =
                textPaintFile.flags or Paint.ANTI_ALIAS_FLAG
        }

        private const val MULTIPLIER_SHIFT = 18
        private const val MULTIPLIER_MASK = 7 shl MULTIPLIER_SHIFT
        private const val MULTIPLIER_BYTES = 0
        private const val MULTIPLIER_KBYTES = 1 shl MULTIPLIER_SHIFT
        private const val MULTIPLIER_MBYTES = 2 shl MULTIPLIER_SHIFT
        private const val MULTIPLIER_MBYTES10 = 3 shl MULTIPLIER_SHIFT
        private const val MULTIPLIER_MBYTES100 = 4 shl MULTIPLIER_SHIFT
        private const val MULTIPLIER_GBYTES = 5 shl MULTIPLIER_SHIFT
        private const val MULTIPLIER_GBYTES10 = 6 shl MULTIPLIER_SHIFT
        private const val MULTIPLIER_GBYTES100 = 7 shl MULTIPLIER_SHIFT
        private const val SIZE_MASK = (1 shl MULTIPLIER_SHIFT) - 1

        //  static int blockSize;
        // will take for a while to make this break
        // 16Mb block size on mobile device... probably in year 2020.
        // probably 32 bits for maximum number of block will break before ~2016
        const val blockOffset = 24
        const val blockMask = (1L shl blockOffset) - 1
        fun calcSizeStringFromEncoded(encodedSize: Long): String {
            val size = SIZE_MASK and encodedSize.toInt()
            return when (MULTIPLIER_MASK and encodedSize.toInt()) {
                MULTIPLIER_BYTES -> String.format(
                    n_bytes!!, size
                )

                MULTIPLIER_KBYTES -> String.format(n_kilobytes!!, size)
                MULTIPLIER_MBYTES -> String.format(n_megabytes!!, size * (1f / 1024))
                MULTIPLIER_MBYTES10 -> String.format(
                    n_megabytes10!!, size * (1f / 1024)
                )

                MULTIPLIER_MBYTES100 -> String.format(
                    n_megabytes100!!, size
                )

                MULTIPLIER_GBYTES -> String.format(n_gigabytes!!, size * (1f / 1024))
                MULTIPLIER_GBYTES10 -> String.format(
                    n_gigabytes10!!, size * (1f / 1024)
                )

                MULTIPLIER_GBYTES100 -> String.format(
                    n_gigabytes100!!, size
                )

                else -> ""
            }
        }

        @JvmStatic
        fun makeNode(
            parent: FileSystemEntry?, name: String
        ): FileSystemEntry {
            return FileSystemEntry(parent, name)
        }

        /**
         * For sorting according to size.
         */
        @JvmField
        val COMPARE = Compare()

        /**
         * Calculate size string for specified file length in bytes.
         * Currently used by delete activity preview file list loader.
         *
         * @param sourceSz file size in bytes
         * @return formated size string
         */
        @JvmStatic
        fun calcSizeString(sourceSz: Float): String {
            var sz = sourceSz
            if (sz < 1024 * 1024 * 10) {
                if (sz < 1024 * 1024) {
                    if (sz < 1024) {
                        if (sz < 0) sz = 0f
                        return String.format(n_bytes!!, sz.toInt())
                    }
                    return String.format(n_kilobytes!!, (sz * (1f / 1024)).toInt())
                }
                return String.format(n_megabytes!!, sz * (1f / 1024 / 1024))
            }
            return if (sz < 1024 * 1024 * 200) {
                String.format(
                    n_megabytes10!!,
                    sz * (1f / 1024 / 1024)
                )
            } else String.format(
                n_megabytes100!!,
                (sz * (1f / 1024 / 1024)).toInt()
            )
        }

        @JvmStatic
        fun setupStrings(context: Context) {
            if (n_bytes != null) return
            n_bytes = context.getString(R.string.n_bytes)
            n_kilobytes = context.getString(R.string.n_kilobytes)
            n_megabytes = context.getString(R.string.n_megabytes)
            n_megabytes10 = context.getString(R.string.n_megabytes10)
            n_megabytes100 = context.getString(R.string.n_megabytes100)
            n_gigabytes = context.getString(R.string.n_gigabytes)
            n_gigabytes10 = context.getString(R.string.n_gigabytes10)
            n_gigabytes100 = context.getString(R.string.n_gigabytes100)
            dir_name_size_num_dirs = context.getString(R.string.dir_name_size_num_dirs)
            dir_empty = context.getString(R.string.dir_empty)
            dir_name_size = context.getString(R.string.dir_name_size)
        }
    }
}
