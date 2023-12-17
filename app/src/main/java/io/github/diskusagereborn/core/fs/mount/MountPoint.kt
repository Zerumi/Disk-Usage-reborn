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
package io.github.diskusagereborn.core.fs.mount

import android.content.Context
import io.github.diskusagereborn.R
import io.github.diskusagereborn.core.data.source.fast.PortableFileImpl.Companion.externalAppFilesDirs
import io.github.diskusagereborn.core.fs.mount.RootMountPoint.Companion.getForKey
import io.github.diskusagereborn.core.fs.mount.RootMountPoint.Companion.initMountPoints
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER

open class MountPoint internal constructor(
    @JvmField var title: String?,
    @JvmField var root: String?,
    open val isDeleteSupported: Boolean
) {
    open val isRootRequired: Boolean
        get() = false
    open val key: String
        get() = "storage:$root"

    open fun hasApps(): Boolean {
        return isDeleteSupported
    }

    companion object {
        private var init = false
        private var mountPoints: MutableList<MountPoint> = ArrayList()
        private var mountPointForKey: MutableMap<String, MountPoint> = HashMap()
        @JvmStatic
        fun getForKey(context: Context, key: String): MountPoint? {
            initMountPoints(context)
            val mountPoint = mountPointForKey[key]
            return mountPoint ?: getForKey(key)
        }

        @JvmStatic
        fun getMountPoints(context: Context): List<MountPoint> {
            initMountPoints(context)
            initMountPoints()
            return mountPoints
        }

        private fun initMountPoints(context: Context) {
            if (init) return
            init = true
            for (dir in externalAppFilesDirs) {
                if (dir == null) continue
                val path = dir.absolutePath
                    ?.replaceFirst("/Android/data/io.github.diskusagereborn/files".toRegex(), "")
                LOGGER.d("MountPoint.initMountPoints: mountpoint %s", path)
                val internal = !dir.isExternalStorageRemovable
                val title = if (internal) context.getString(R.string.storage_card) else path
                val mountPoint = MountPoint(title, path, internal)
                mountPoints.add(mountPoint)
                mountPointForKey[mountPoint.key] = mountPoint
            }
        }

        @JvmStatic
        fun reset() {
            mountPoints = ArrayList()
            mountPointForKey = HashMap()
            init = false
            RootMountPoint.reset()
        }
    }
}
