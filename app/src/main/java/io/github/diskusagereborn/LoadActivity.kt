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
package io.github.diskusagereborn

import android.app.ActivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.diskusagereborn.core.Apps2SDLoader
import io.github.diskusagereborn.core.data.source.fast.LegacyFileImpl.Companion.createRoot
import io.github.diskusagereborn.core.fs.FileSystemStats
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry
import io.github.diskusagereborn.core.fs.entity.FileSystemFreeSpace
import io.github.diskusagereborn.core.fs.entity.FileSystemRoot
import io.github.diskusagereborn.core.fs.entity.FileSystemRoot.Companion.makeNode
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot
import io.github.diskusagereborn.core.fs.entity.FileSystemSystemSpace
import io.github.diskusagereborn.core.fs.mount.MountPoint.Companion.getForKey
import io.github.diskusagereborn.core.fs.mount.MountPoint.Companion.getMountPoints
import io.github.diskusagereborn.core.scanner.NativeScanner
import io.github.diskusagereborn.core.scanner.Scanner
import io.github.diskusagereborn.ui.theme.DiskUsageTheme
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Arrays
import java.util.Collections

open class LoadActivity : ComponentActivity() {

    private lateinit var key : String

    private lateinit var updateProgress: (Float, String) -> Unit
    private val memoryMaxHeap: Int
        get() = run {
            val manager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            return manager.memoryClass * 1024 * 1024
        }
    private val memoryQuota: Int
        get() {
            val totalMem = memoryMaxHeap
            val numMountPoints = getMountPoints(this).size
            return totalMem / (numMountPoints + 1)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        key = intent.getStringExtra(DiskUsageApplication.KEY_KEY).toString()
        FileSystemEntry.setupStrings(this)
        setContent {
            DiskUsageTheme {
                LinearDeterminateIndicator(::loadProgress) { finish() }
            }
        }
    }

    /** Iterate the progress value */
    private suspend fun loadProgress(updateProgress: (Float, String) -> Unit) {
        startLoadDirectories(updateProgress = updateProgress)
    }
    private suspend fun startLoadDirectories(updateProgress: (Float, String) -> Unit) {
        val mountPoint = getForKey(this, key)
        val stats = FileSystemStats(mountPoint!!)
        val heap = memoryQuota
        var rootElement: FileSystemEntry = try {
            val scanner = NativeScanner(stats.blockSize, stats.busyBlocks, heap)
            scanner.scan(mountPoint)!!
        } catch (e: Exception) {
            val scanner = Scanner(20, stats.blockSize, stats.busyBlocks, heap)
            scanner.scan(createRoot(mountPoint.root))!!
        }

        var entries = ArrayList<FileSystemEntry?>()
        if (rootElement.children != null) {
            rootElement.children?.let { Collections.addAll(entries, *it) }
        }
        if (mountPoint.hasApps()) {
            val media = makeNode(
                getString(R.string.graph_media), mountPoint.root!!, false
            ).setChildren(
                entries.toTypedArray<FileSystemEntry?>(),
                stats.blockSize
            ) as FileSystemRoot
            entries = ArrayList()
            entries.add(media)
            val appList = loadApps2SD(stats.blockSize, updateProgress)

            LOGGER.i("applist size ${appList?.size}")

            /* if (appList != null) {
                moveAppData(appList, media, stats.blockSize)
                val apps = makeNode(null, getString(R.string.graph_apps)).setChildren(
                    appList as Array<FileSystemEntry?>,
                    stats.blockSize
                )
                entries.add(apps)
            } */
        }
        LOGGER.i("loadProcess() - key: $key")

        var visibleBlocks: Long = 0
        for (e in entries) {
            visibleBlocks += e!!.sizeInBlocks
        }
        val systemBlocks = stats.totalBlocks - stats.freeBlocks - visibleBlocks
        Collections.sort(entries, FileSystemEntry.COMPARE)
        if (systemBlocks > 0) {
            entries.add(
                FileSystemSystemSpace(
                    getString(R.string.graph_system_data),
                    systemBlocks * stats.blockSize,
                    stats.blockSize
                )
            )
            entries.add(
                FileSystemFreeSpace(
                    getString(R.string.graph_free_space),
                    stats.freeBlocks * stats.blockSize,
                    stats.blockSize
                )
            )
        } else {
            val freeBlocks = stats.freeBlocks + systemBlocks
            if (freeBlocks > 0) {
                entries.add(
                    FileSystemFreeSpace(
                        getString(R.string.graph_free_space),
                        freeBlocks * stats.blockSize,
                        stats.blockSize
                    )
                )
            }
        }
        rootElement = makeNode(
            mountPoint.title, mountPoint.root!!, false
        )
            .setChildren(entries.toTypedArray<FileSystemEntry?>(), stats.blockSize)
        val newRoot = FileSystemSuperRoot(stats.blockSize)
        newRoot.setChildren(arrayOf(rootElement), stats.blockSize)

        // return newRoot
    }

    private suspend fun loadApps2SD(blockSize: Long, updateProgress: (Float, String) -> Unit): Array<FileSystemEntry>? {
        return try {
            Apps2SDLoader(this, updateProgress).load(blockSize)
        } catch (t: Throwable) {
            LOGGER.e("DiskUsage.loadApps2SD(): Problem loading apps2sd info", t)
            null
        }
    }

}
@Composable
fun LinearDeterminateIndicator(
    progressBarUpdater : suspend ((Float, String) -> Unit) -> Unit,
    onDismissRequest : () -> Unit) {
    var currentProgress by remember { mutableStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    var currentFile by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope() // Create a coroutine scope

    LaunchedEffect(scope) {
        scope.launch {
            progressBarUpdater { progress, string ->
                currentProgress = progress
                currentFile = string
            }
            loading = false // Reset loading when the coroutine finishes
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Scanning directories...",
                    modifier = Modifier
                        .padding(4.dp)
                )
                LinearProgressIndicator(
                    progress = currentProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Current: ",
                        modifier = Modifier
                            .padding(3.dp)
                    )
                    Text(
                        text = currentFile,
                        modifier = Modifier
                            .padding(3.dp)
                    )
                }
            }
        }
    }
}

