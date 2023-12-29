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

import io.github.diskusagereborn.core.data.source.LegacyFile
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.IOException

class LegacyFileImpl private constructor(private val file: File) : LegacyFile {

    override val name: String?
        get() = file.name

    @get:Throws(IOException::class)
    override val canonicalPath: String?
        get() = file.canonicalPath


    override val isLink: Boolean
        get() = try {
            file.canonicalPath != file.path
        } catch (e: Throwable) {
            true
        }

    override val isFile: Boolean
        get() = file.isFile

    override fun length(): Long {
        return file.length()
    }

    override fun listFiles(): Array<LegacyFile?> {
        val children = file.listFiles()
        val res = arrayOfNulls<LegacyFile>(children?.size ?: 0)
        if (children != null) {
            for (i in children.indices) {
                res[i] = LegacyFileImpl(children[i])
            }
        }
        return res
    }

    override fun list(): Array<String?>? {
        return file.list()
    }

    override fun getChild(childName: String?): LegacyFile {
        return LegacyFileImpl(File(file, childName))
    }

    companion object {
        @JvmStatic
        @Contract("_ -> new")
        fun createRoot(root: String?): LegacyFile {
            return LegacyFileImpl(File(root!!))
        }
    }
}
