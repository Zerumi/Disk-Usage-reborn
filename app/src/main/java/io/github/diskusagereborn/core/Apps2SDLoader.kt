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
package io.github.diskusagereborn.core

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.storage.StorageManager
import io.github.diskusagereborn.LoadActivity
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry
import io.github.diskusagereborn.core.fs.entity.FileSystemPackage
import io.github.diskusagereborn.ui.load.ScannerAdapter
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER
import kotlinx.coroutines.delay
import java.util.Arrays

class Apps2SDLoader(private val loadActivity: LoadActivity, val callUpdate: ScannerAdapter) {
    private var lastAppName: CharSequence = ""
    private var numLoadedPackages = 0
    private var pos = callUpdate.getCurrentPos()

    suspend fun load(blockSize: Long): Array<FileSystemEntry> {
        val usageStatsManager =
            loadActivity.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val queryUsageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_YEARLY, 0, System.currentTimeMillis()
        )
        LOGGER.d("Apps2SDLoader.load(): Stats size = %s", queryUsageStats.size)
        val storageStatsManager =
            loadActivity.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val entries = ArrayList<FileSystemEntry>()
        val packageManager = loadActivity.applicationContext.packageManager
        val packages: MutableSet<String> = HashSet()
        //val size : Float = queryUsageStats.size.toFloat()
        for (s in queryUsageStats) {
            packages.add(s.packageName)
        }
        for (pkg in packages) {
            LOGGER.d("app: $pkg")
            try {
                val metadata = packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                val appName = metadata.loadLabel(packageManager).toString()
                lastAppName = appName
                val stats = storageStatsManager.queryStatsForPackage(
                    StorageManager.UUID_DEFAULT, pkg, Process.myUserHandle()
                )
                LOGGER.d("stats: " + stats.appBytes + " " + stats.dataBytes)
                val p = FileSystemPackage(
                    appName,
                    pkg,
                    stats.appBytes,
                    stats.dataBytes,
                    stats.cacheBytes,
                    metadata.flags
                )
                p.applyFilter(blockSize)
                entries.add(p)
                numLoadedPackages++
                pos += p.sizeInBlocks
                callUpdate.sourceUpdate(pos, appName)
                delay(1)
            } catch (e: PackageManager.NameNotFoundException) {
                LOGGER.d("Failed to get package", e)
            }
        }
        val result = entries.toArray(arrayOf<FileSystemEntry>())
        Arrays.sort(result, FileSystemEntry.COMPARE)
        return result
    }
}
