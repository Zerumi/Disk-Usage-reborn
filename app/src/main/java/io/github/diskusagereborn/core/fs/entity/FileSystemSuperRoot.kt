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

/**
 * Non displayed entry which contains just one entry which is
 * displayed root of filesystem.
 */
class FileSystemSuperRoot(private val displayBlockSize: Long) :
    FileSystemSpecial("", 0, displayBlockSize) {

    override fun create(): FileSystemEntry {
        return FileSystemSuperRoot(displayBlockSize)
    }

    override fun filter(pattern: CharSequence?, blockSize: Long): FileSystemEntry? {
        // don't match name
        return filterChildren(pattern, blockSize)
    }

    /* fun getByAbsolutePath(path: String?): FileSystemEntry? {
        for (r in children!!) {
            if (r !is FileSystemRoot) {
                continue
            }
            val e: FileSystemEntry? = r.getByAbsolutePath(path!!)
            if (e != null) {
                return e
            }
        }
        return null
    } */

    override fun getEntryByName(path: String, exactMatch: Boolean): FileSystemEntry? {
        return children?.get(0)!!.getEntryByName(path, exactMatch)
    }
}
