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
package io.github.diskusagereborn

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.diskusagereborn.R
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry.Companion.setupStrings
import io.github.diskusagereborn.core.fs.entity.FileSystemPackage
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot
import io.github.diskusagereborn.ui.theme.DiskUsageTheme
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.TreeMap

/* fun interface AfterLoad {
    fun run(root: FileSystemSuperRoot?, isCached: Boolean)
} */

abstract class LoadActivity : ComponentActivity() {
    // var pkg_removed: FileSystemPackage? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStrings(this)
        setContent {
            DiskUsageTheme {
                LinearDeterminateIndicator() {}
            }
        }
    }
    /*
    abstract val key: String
    @Throws(IOException::class, InterruptedException::class)
    abstract fun scan(): FileSystemSuperRoot
    class PersistantActivityState {
        var root: FileSystemSuperRoot? = null
        var afterLoad: AfterLoad? = null
    }

    val persistantState: PersistantActivityState
        get() {
            val key = key
            var state = persistantActivityState[key]
            if (state != null) return state
            state = PersistantActivityState()
            persistantActivityState[key] = state
            return state
        }

    fun LoadFiles(
        activity: LoadableActivity,
        runAfterLoad: AfterLoad, force: Boolean
    ) {
        val scanRunning: Boolean
        val state = persistantState
        LOGGER.d("LoadableActivity.LoadFiles(), afterLoad = %s", runAfterLoad)
        if (force) {
            state.root = null
        }
        if (state.root != null) {
            runAfterLoad.run(state.root, true)
            return
        }
        scanRunning = state.afterLoad != null
        state.afterLoad = runAfterLoad
        LOGGER.d("LoadableActivity.LoadFiles(): Created new progress dialog")
        state.loading = ScanProgressDialog(activity)
        val thisLoading = state.loading
        state.loading!!.setOnCancelListener { dialog: DialogInterface? ->
            state.loading = null
            activity.finish()
        }
        thisLoading!!.setCancelable(true)
        //    thisLoading.setIndeterminate(true);
        thisLoading.setMax(1)
        thisLoading.setMessage(activity.getString(R.string.scaning_directories))
        thisLoading.show()
        if (scanRunning) return
        val handler = Handler()
        object : Thread() {
            override fun run() {
                val error: String
                try {
                    LOGGER.d("LoadableActivity.LoadFiles(): Running scan for %s", key)
                    val newRoot = scan()
                    handler.post {
                        if (state.loading == null) {
                            LOGGER.d("LoadableActivity.LoadFiles(): No dialog, doesn't run afterLoad")
                            state.afterLoad = null
                            if (newRoot.children!![0]!!.children != null) {
                                LOGGER.d("LoadableActivity.LoadFiles(): No dialog, updating root still")
                                state.root = newRoot
                            }
                            return@post
                        }
                        if (state.loading!!.isShowing) state.loading!!.dismiss()
                        state.loading = null
                        val afterLoadCopy = state.afterLoad
                        state.afterLoad = null
                        LOGGER.d("LoadableActivity.LoadFiles(): Dismissed dialog")
                        if (newRoot.children!![0]!!.children == null) {
                            LOGGER.d("LoadableActivity.LoadFiles(): Empty card")
                            handleEmptySDCard(activity, runAfterLoad)
                            return@post
                        }
                        state.root = newRoot
                        pkg_removed = null
                        LOGGER.d("LoadableActivity.LoadFiles(): Run afterLoad = %s", afterLoadCopy)
                        afterLoadCopy!!.run(state.root, false)
                    }
                    return
                } catch (e: OutOfMemoryError) {
                    state.root = null
                    state.afterLoad = null
                    LOGGER.d("LoadableActivity.LoadFiles(): Out of memory!")
                    handler.post {
                        if (state.loading == null) return@post
                        state.loading!!.dismiss()
                        handleOutOfMemory(activity)
                    }
                    return
                } catch (e: InterruptedException) {
                    error = e.javaClass.name + ":" + e.message
                    LOGGER.e("LoadableActivity.LoadFiles(): Native error", e)
                } catch (e: IOException) {
                    error = e.javaClass.name + ":" + e.message
                    LOGGER.e("LoadableActivity.LoadFiles(): Native error", e)
                } catch (e: RuntimeException) {
                    error = e.javaClass.name + ":" + e.message
                    LOGGER.e("LoadableActivity.LoadFiles(): Native error", e)
                } catch (e: StackOverflowError) {
                    error = "Filesystem is damaged."
                }
                state.root = null
                state.afterLoad = null
                LOGGER.d("LoadableActivity.LoadFiles(): Exception in scan!")
                handler.post {
                    if (state.loading == null) return@post
                    state.loading!!.dismiss()
                    AlertDialog.Builder(activity)
                        .setTitle(error)
                        .setOnCancelListener { dialog: DialogInterface? -> activity.finish() }
                        .show()
                }
            }
        }.start()
    }

    override fun onPause() {
        val state = persistantState
        if (state.loading != null) {
            if (state.loading!!.isShowing) state.loading!!.dismiss()
            LOGGER.d("LoadableActivity.onPause(): Removed progress dialog")
            state.loading = null
        }
        super.onPause()
    }

    private fun handleEmptySDCard(
        activity: LoadableActivity,
        afterLoad: AfterLoad?
    ) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.empty_or_missing_sdcard))
            .setPositiveButton(activity.getString(R.string.button_rescan)) { dialog: DialogInterface?, which: Int ->
                if (afterLoad == null) throw RuntimeException("LoadableActivity.handleEmptySDCard(): afterLoad is empty")
                LoadFiles(activity, afterLoad, true)
            }
            .setOnCancelListener { dialog: DialogInterface? -> activity.finish() }.create().show()
    }

    companion object {
        private val persistantActivityState: MutableMap<String, PersistantActivityState> = TreeMap()
        fun resetStoredStates() {
            persistantActivityState.clear()
        }

        // FIXME: use it wisely
        fun forceCleanup(): Boolean {
            var success = false
            for (state in persistantActivityState.values) {
                if (state.afterLoad == null && state.root != null) {
                    state.root = null
                    success = true
                }
            }
            return success
        }

        private fun handleOutOfMemory(activity: Activity) {
            try {
                // Can fail if the main window is already closed.
                AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.out_of_memory))
                    .setOnCancelListener { dialog: DialogInterface? -> activity.finish() }.create()
                    .show()
            } catch (t: Throwable) {
                toast("DiskUsage is out of memory. Sorry.")
            }
        }
    }*/
}
