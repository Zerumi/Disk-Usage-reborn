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

import android.os.Environment
import io.github.diskusagereborn.core.data.source.PortableFile
import io.github.diskusagereborn.utils.PathHelper.getExternalAppFilesPaths
import java.io.File

class PortableFileImpl private constructor(private val file: File) : PortableFile {

    override val isExternalStorageEmulated: Boolean
        get() = try {
            Environment.isExternalStorageEmulated(file)
        } catch (e: Exception) {
            false
        }

    override val isExternalStorageRemovable: Boolean
        get() = try {
            Environment.isExternalStorageRemovable(file)
        } catch (e: Exception) {
            false
        }

    override val canonicalPath: String?
        get() = try {
            file.canonicalPath
        } catch (e: Exception) {
            file.absolutePath
        }

    override val absolutePath: String?
        get() = file.absolutePath

    override val totalSpace: Long
        get() = file.totalSpace

    override fun equals(other: Any?): Boolean {
        if (other !is PortableFile) {
            return false
        }
        return other.absolutePath == absolutePath
    }

    override fun hashCode(): Int {
        return absolutePath.hashCode()
    }

    companion object {
        private fun make(file: File?): PortableFileImpl? {
            return file?.let { PortableFileImpl(it) }
        }

        @JvmStatic
        val externalAppFilesDirs: Array<PortableFile?>
            get() {
                val externalAppFilesPaths: Array<out File> = getExternalAppFilesPaths()
                val result = arrayOfNulls<PortableFile>(externalAppFilesPaths.size)
                var i = 0
                for (dir in externalAppFilesPaths) {
                    result[i++] = make(dir)
                }
                return result
            }
    }
}