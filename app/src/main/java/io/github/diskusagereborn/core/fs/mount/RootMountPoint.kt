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
package io.github.diskusagereborn.core.fs.mount

import io.github.diskusagereborn.utils.IOHelper.getProcMountsReader
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER

class RootMountPoint  // private final String fsType;
internal constructor(root: String?) : MountPoint(root, root, false) {

    override val isRootRequired: Boolean
        get() = true

    override fun hasApps(): Boolean {
        return false
    }

    override val isDeleteSupported: Boolean
        get() = false

    override val key: String
        get() = "rooted:$root"

    companion object {
        private var rootedMountPoints: MutableList<MountPoint> = ArrayList()
        private var rootedMountPointForKey: MutableMap<String, MountPoint> = HashMap()
        private var init = false
        @JvmField
        var checksum = 0
        /* @JvmStatic
        fun getRootedMountPoints(): List<MountPoint> {
            initMountPoints()
            return rootedMountPoints
        } */

        @JvmStatic
        fun getForKey(key: String): MountPoint? {
            initMountPoints()
            return rootedMountPointForKey[key]
        }

        @JvmStatic
        fun initMountPoints() {
            if (init) return
            init = true
            try {
                checksum = 0
                val reader = getProcMountsReader()
                var line: String
                while (reader.readLine().also { line = it } != null) {
                    checksum += line.length
                    LOGGER.d("RootMountPoint.initMountPoints(): Line: %s", line)
                    val parts =
                        line.split(" +".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (parts.size < 3) continue
                    val mountPoint = parts[1]
                    LOGGER.d("RootMountPoint.initMountPoints(): Mount point: $mountPoint")
                    if (!mountPoint.startsWith("/mnt/asec/")) {
                        val m: MountPoint = RootMountPoint(mountPoint)
                        rootedMountPoints.add(m)
                        rootedMountPointForKey[m.key] = m
                    }
                }
                reader.close()
            } catch (e: Exception) {
                LOGGER.e("RootMountPoint.initMountPoints(): Failed to get mount points", e)
            }
        }

        /* @JvmStatic
        fun reset() {
            rootedMountPoints = ArrayList()
            rootedMountPointForKey = HashMap()
            init = false
        } */
    }
}
