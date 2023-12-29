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

import org.jetbrains.annotations.Contract

class FileSystemRoot private constructor(
    name: String?,
    val rootPath: String,
    override val isDeletable: Boolean
) : FileSystemEntry(null, name ?: "") {

    override fun create(): FileSystemEntry {
        return FileSystemRoot(name, rootPath, isDeletable)
    }

    override fun filter(pattern: CharSequence?, blockSize: Long): FileSystemEntry? {
        // don't match name
        return filterChildren(pattern, blockSize)
    }

    fun getByAbsolutePath(path: String): FileSystemEntry? {
        val rootPathWithSlash = withSlash(
            rootPath
        )
        val pathWithSlash = withSlash(path)
        if (pathWithSlash == rootPathWithSlash) {
            return getEntryByName(path, true)
        }
        if (pathWithSlash.startsWith(rootPathWithSlash)) {
            return getEntryByName(path.substring(rootPathWithSlash.length), true)
        }
        for (s in children!!) {
            if (s is FileSystemRoot) {
                val e: FileSystemEntry? = s.getByAbsolutePath(path)
                if (e != null) return e
            }
        }
        return null
    }

    companion object {
        @JvmStatic
        @Contract("_, _, _ -> new")
        fun makeNode(name: String?, rootPath: String, deletable: Boolean): FileSystemRoot {
            return FileSystemRoot(name, rootPath, deletable)
        }

        fun withSlash(sourcePath: String): String {
            var path = sourcePath
            if (path.isNotEmpty() && path[path.length - 1] != '/') path += '/'
            return path
        }
    }
}
