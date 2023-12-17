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
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.FileUriExposedException
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import io.github.diskusagereborn.core.scanner.NativeScanner
import io.github.diskusagereborn.core.scanner.Scanner
//import io.github.diskusagereborn.core.data.source.StatFsSource
//import io.github.diskusagereborn.core.data.source.fast.LegacyFileImpl.Companion.createRoot
//import io.github.diskusagereborn.core.data.source.fast.StatFsSourceImpl
//import io.github.diskusagereborn.core.fs.Apps2SDLoader
//import io.github.diskusagereborn.core.fs.BackgroundDelete.Companion.startDelete
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry.Companion.calcSizeString
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry.Companion.makeNode
import io.github.diskusagereborn.core.fs.entity.FileSystemEntrySmall
import io.github.diskusagereborn.core.fs.entity.FileSystemFreeSpace
import io.github.diskusagereborn.core.fs.entity.FileSystemPackage
import io.github.diskusagereborn.core.fs.entity.FileSystemPackage.ChildType
import io.github.diskusagereborn.core.fs.entity.FileSystemRoot
import io.github.diskusagereborn.core.fs.entity.FileSystemRoot.Companion.makeNode
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot
import io.github.diskusagereborn.core.fs.entity.FileSystemSystemSpace
import io.github.diskusagereborn.core.fs.mount.MountPoint
import io.github.diskusagereborn.core.fs.mount.MountPoint.Companion.getForKey
import io.github.diskusagereborn.core.fs.mount.MountPoint.Companion.getMountPoints
import io.github.diskusagereborn.utils.Logger.Companion.LOGGER
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.IOException
import java.util.Arrays
import java.util.Collections
import java.util.Locale

open class LoadActivityImpl : LoadActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    /* override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        LOGGER.d("DiskUsage.onCreate()")
        val binding = ActivityCommonBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        menu.onCreate()
        val i = intent
        key = i.getStringExtra(KEY_KEY).toString()
        val receivedState = i.getBundleExtra(STATE_KEY)
        val mountPoint = getForKey(this, key)
        if (mountPoint == null) {
            finish()
            return
        }
        LOGGER.d(
            "DiskUsage.onCreate(), rootPath = %s, receivedState = %s",
            mountPoint.root, receivedState
        )
        receivedState?.let { onRestoreInstanceState(it) }
    } */

    // FIXME: wrap to direct requests to rendering thread
    /*
    @JvmField
    var fileSystemState: FileSystemState? = null
    private var pathToDelete: String? = null
    @JvmField
    val menu = DiskUsageMenu.getInstance(this)
    @JvmField
    val rendererManager = RendererManager(this)


    val afterLoadAction = ArrayList<Runnable>()
    fun applyPatternNewRoot(newRoot: FileSystemSuperRoot?, searchQuery: String?) {
        if (newRoot != null) {
            fileSystemState!!.replaceRootKeepCursor(newRoot, searchQuery)
        }
    }

    override fun onResume() {
        super.onResume()
        rendererManager.onResume()
        if (pkg_removed != null) {
            // Check if package removed
            val pkg_name = pkg_removed!!.pkg
            val pm = packageManager
            try {
                pm.getPackageInfo(pkg_name, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                if (fileSystemState != null) fileSystemState!!.removeInRenderThread(pkg_removed!!)
            }
            pkg_removed = null
        }
        LoadFiles(this, { root: FileSystemSuperRoot?, isCached: Boolean ->
            fileSystemState = FileSystemState(this@DiskUsage, root!!)
            rendererManager.makeView(fileSystemState, root)
            fileSystemState!!.startZoomAnimationInRenderThread(null, !isCached, false)
            for (r in afterLoadAction) {
                r.run()
            }
            afterLoadAction.clear()
            if (pathToDelete != null) {
                val path: String = pathToDelete!!
                pathToDelete = null
                continueDelete(path)
            }
        }, false)
    }

    override fun onPause() {
        rendererManager.onPause()
        super.onPause()
        if (fileSystemState != null) {
            fileSystemState!!.killRenderThread()
            val savedState = Bundle()
            fileSystemState!!.saveState(savedState)
            afterLoadAction.add(Runnable { fileSystemState!!.restoreStateInRenderThread(savedState) })
        }
    }

    public override fun onActivityResult(a: Int, result: Int, i: Intent) {
        super.onActivityResult(a, result, i)
        if (result != RESULT_DELETE_CONFIRMED) return
        pathToDelete = i.getStringExtra("path")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    private abstract class VersionedPackageViewer {
        abstract fun viewPackage(pkg: String)

        companion object {
            fun newInstance(context: DiskUsage): VersionedPackageViewer {
                return context.GingerbreadPackageViewer()
            }
        }
    }

    private inner class GingerbreadPackageViewer : VersionedPackageViewer() {
        override fun viewPackage(pkg: String) {
            LOGGER.d("Show package = %s", pkg)
            val viewIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$pkg")
            )
            startActivity(viewIntent)
        }
    }

    private val packageViewer = VersionedPackageViewer.newInstance(this)
    protected fun viewPackage(pkg: FileSystemPackage) {
        packageViewer.viewPackage(pkg.pkg)
        // FIXME: reload package data instead of just removing it
        pkg_removed = pkg
    }

    fun continueDelete(path: String?) {
        val entry = fileSystemState!!.masterRoot.getEntryByName(path!!, true)
        if (entry != null) {
            startDelete(this, entry)
        } else {
            toast("Oops. Can't find directory to be deleted.")
        }
    }

    @OptIn(UnreliableToastApi::class)
    fun askForDeletion(entry: FileSystemEntry) {
        val path = entry.path2()
        val fullPath = entry.absolutePath()
        LOGGER.d("Deletion requested for %s", path)
        if (entry is FileSystemEntrySmall) {
            toast("Delete directory instead")
            return
        }
        if (entry.children == null || entry.children!!.size == 0) {
            if (entry is FileSystemPackage) {
                pkg_removed = entry
                startDelete(this, entry)
                return
            }

            // Delete single file or directory
            AlertDialog.Builder(this)
                .setTitle(
                    if (File(fullPath).isDirectory) getString(
                        R.string.ask_to_delete_directory,
                        path
                    ) else getString(R.string.ask_to_delete_file, path)
                )
                .setPositiveButton(
                    R.string.button_delete
                ) { dialog: DialogInterface?, which: Int -> startDelete(this@DiskUsage, entry) }
                .setNegativeButton(android.R.string.cancel, null).create().show()
        } else {
            val i = Intent(this, DeleteActivity::class.java)
            i.putExtra(DELETE_PATH_KEY, path)
            i.putExtra(DELETE_ABSOLUTE_PATH_KEY, fullPath)
            i.putExtra(DeleteActivity.NUM_FILES_KEY, entry.numFiles)
            i.putExtra(KEY_KEY, key)
            i.putExtra(DeleteActivity.SIZE_KEY, entry.sizeString())
            this.startActivityForResult(i, 0)
        }
    }

    fun isIntentAvailable(intent: Intent?): Boolean {
        val packageManager = packageManager
        val res = packageManager.queryIntentActivities(
            intent!!, PackageManager.MATCH_DEFAULT_ONLY
        )
        return res.size > 0
    }

    fun view(entry: FileSystemEntry) {
        var entry = entry
        var intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        if (entry is FileSystemEntrySmall) {
            entry = entry.parent!!
        }
        if (entry is FileSystemPackage) {
            viewPackage(entry)
            return
        }
        if (entry.parent is FileSystemPackage) {
            viewPackage((entry.parent as FileSystemPackage?)!!)
            return
        }
        val path = entry.absolutePath()
        val file = File(path)
        var uri = Uri.fromFile(file)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
        }
        if (file.isDirectory) {
            // Go on with default file manager
            // Shoud this be optional?
            intent = Intent(Intent.ACTION_VIEW)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setDataAndType(uri, "inode/directory")
            try {
                startActivity(intent)
                return
            } catch (ignored: ActivityNotFoundException) {
            }
            intent = Intent("org.openintents.action.VIEW_DIRECTORY")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setData(uri)
            try {
                startActivity(intent)
                return
            } catch (ignored: ActivityNotFoundException) {
            } catch (ignored: FileUriExposedException) {
            }
            intent = Intent("org.openintents.action.PICK_DIRECTORY")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setData(uri)
            intent.putExtra(
                "org.openintents.extra.TITLE",
                getString(R.string.title_in_oi_file_manager)
            )
            intent.putExtra(
                "org.openintents.extra.BUTTON_TEXT",
                getString(R.string.button_text_in_oi_file_manager)
            )
            try {
                startActivity(intent)
                return
            } catch (ignored: ActivityNotFoundException) {
            } catch (ignored: FileUriExposedException) {
            }

            // old Astro
            intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setDataAndType(uri, "vnd.android.cursor.item/com.metago.filemanager.dir")
            try {
                startActivity(intent)
                return
            } catch (ignored: ActivityNotFoundException) {
            } catch (ignored: FileUriExposedException) {
            }
            toast(R.string.no_viewer_found)
            return
        }
        val fileName = entry.name
        val dot = fileName.lastIndexOf(".")
        Log.d("diskusage", "name: $fileName path: $path dot: $dot")
        if (dot != -1) {
            val extension = fileName.substring(dot + 1).lowercase(Locale.getDefault())
            val mimeTypeMap = MimeTypeMap.getSingleton()
            val mime = mimeTypeMap.getMimeTypeFromExtension(extension)
            Log.d("diskusage", "extension: $extension mime: $mime")
            try {
                intent = Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                if (mime != null) {
                    intent.setDataAndType(uri, mime)
                } else {
                    intent.setDataAndType(uri, "binary/octet-stream")
                }
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.e("diskusage", "Can't open viewer and crash", e)
            } catch (e: FileUriExposedException) {
                Log.e("diskusage", "Can't open viewer and crash", e)
            }
        }
        toast(R.string.no_viewer_found)
    }

    fun rescan() {
        LoadFiles(
            this@DiskUsage,
            AfterLoad { newRoot: FileSystemSuperRoot?, isCached: Boolean ->
                fileSystemState!!.startZoomAnimationInRenderThread(
                    newRoot,
                    !isCached,
                    false
                )
            },
            true
        )
    }

    fun finishOnBack() {
        if (!menu.readyToFinish()) {
            return
        }
        val outState = Bundle()
        onSaveInstanceState(outState)
        val result = Intent()
        result.putExtra(STATE_KEY, outState)
        result.putExtra(KEY_KEY, key)
        setResult(0, result)
        finish()
    }

    fun setSelectedEntity(position: FileSystemEntry) {
        menu.update(position)
        title = getString(R.string.title_for_path, position.toTitleString())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finishOnBack()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (fileSystemState == null) return
        fileSystemState!!.killRenderThread()
        fileSystemState!!.saveState(outState)
        menu.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(inState: Bundle) {
        LOGGER.d("DiskUsage.onRestoreInstanceState(), rootPath = %s", inState.getString(KEY_KEY))
        if (fileSystemState != null) fileSystemState!!.restoreStateInRenderThread(inState) else {
            afterLoadAction.add(Runnable { fileSystemState!!.restoreStateInRenderThread(inState) })
        }
        menu.onRestoreInstanceState(inState)
    }

    fun interface AfterLoad {
        fun run(root: FileSystemSuperRoot?, isCached: Boolean)
    }

    @JvmField
    val handler = Handler()

    abstract class MemoryClass {
        abstract fun maxHeap(): Int

        companion object {
            @Contract("_ -> new")
            fun getInstance(diskUsage: DiskUsage): MemoryClass {
                return diskUsage.MemoryClassDetected()
            }
        }
    }

    internal inner class MemoryClassDetected : MemoryClass() {
        override fun maxHeap(): Int {
            val manager = this@DiskUsage.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            return manager.memoryClass * 1024 * 1024
        }
    }

    val memoryClass = MemoryClass.getInstance(this)
    private val memoryQuota: Int
        get() {
            val totalMem = memoryClass.maxHeap()
            val numMountPoints = getMountPoints(this).size
            return totalMem / (numMountPoints + 1)
        }

    class FileSystemStats(mountPoint: MountPoint) {
        var blockSize: Long = 0
        var freeBlocks: Long = 0
        var busyBlocks: Long = 0
        var totalBlocks: Long = 0

        init {
            var stats: StatFsSource? = null
            try {
                stats = StatFsSourceImpl(mountPoint.root)
            } catch (e: IllegalArgumentException) {
                LOGGER.e(e, "Failed to get filesystem stats for " + mountPoint.root)
            }
            if (stats != null) {
                blockSize = stats.blockSizeLong
                freeBlocks = stats.availableBlocksLong
                totalBlocks = stats.blockCountLong
                busyBlocks = totalBlocks - freeBlocks
            } else {
                busyBlocks = 0
                totalBlocks = busyBlocks
                freeBlocks = totalBlocks
                blockSize = 512
            }
        }

        fun formatUsageInfo(): String {
            return if (totalBlocks == 0L) "Used <no information>" else String.format(
                "Used %s of %s",
                calcSizeString((busyBlocks * blockSize).toFloat()),
                calcSizeString((totalBlocks * blockSize).toFloat())
            )
        }
    }

    interface ProgressGenerator {
        fun lastCreatedFile(): FileSystemEntry?
        fun pos(): Long
    }

    fun makeProgressUpdater(
        scanner: ProgressGenerator,
        stats: FileSystemStats
    ): Runnable {
        return object : Runnable {
            private var file: FileSystemEntry? = null
            override fun run() {
                val dialog = persistantState.loading
                if (dialog != null) {
                    dialog.setMax(stats.busyBlocks)
                    val lastFile = scanner.lastCreatedFile()
                    if (lastFile !== file) {
                        dialog.setProgress(scanner.pos(), lastFile)
                    }
                    file = lastFile
                }
                handler.postDelayed(this, 50)
            }
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    public override fun scan(): FileSystemSuperRoot {
        val mountPoint = getForKey(this, key!!)
        val stats = FileSystemStats(mountPoint!!)
        val heap = memoryQuota
        var rootElement: FileSystemEntry
        var progressUpdater: Runnable
        try {
            val scanner = NativeScanner(stats.blockSize, stats.busyBlocks, heap)
            progressUpdater = makeProgressUpdater(scanner, stats)
            handler.post(progressUpdater)
            rootElement = scanner.scan(mountPoint)!!
            handler.removeCallbacks(progressUpdater)
        } catch (e: RuntimeException) {
            val scanner = Scanner(20, stats.blockSize, stats.busyBlocks, heap)
            progressUpdater = makeProgressUpdater(scanner, stats)
            handler.post(progressUpdater)
            rootElement = scanner.scan(createRoot(mountPoint.root))!!
            handler.removeCallbacks(progressUpdater)
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
            val appList = loadApps2SD(stats.blockSize)
            if (appList != null) {
                moveAppData(appList, media, stats.blockSize)
                val apps = makeNode(null, getString(R.string.graph_apps)).setChildren(
                    appList as Array<FileSystemEntry?>,
                    stats.blockSize
                )
                entries.add(apps)
            }
        }
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
        return newRoot
    }

    protected fun loadApps2SD(blockSize: Long): Array<FileSystemEntry>? {
        return try {
            Apps2SDLoader(this).load(blockSize)
        } catch (t: Throwable) {
            LOGGER.e("DiskUsage.loadApps2SD(): Problem loading apps2sd info", t)
            null
        }
    }

    fun moveIntoPackage(
        pkg: FileSystemPackage?,
        root: FileSystemRoot,
        path: String?, newName: String?,
        type: ChildType?,
        blockSize: Long
    ) {
        val e = root.getByAbsolutePath(path!!)
        if (e != null) {
            e.remove(blockSize)
            val newRoot = makeNode(newName, path, true)
            e.children?.let { newRoot.setChildren(it, blockSize) }
            pkg!!.addPublicChild(newRoot, type!!, blockSize)
        }
    }

    fun moveAppData(apps: Array<FileSystemEntry>, media: FileSystemRoot, blockSize: Long) {
        val diskusage = "com.google.android.diskusage"
        for (a in apps) {
            val app = a as FileSystemPackage?
            try {
                val cacheDir = cacheDir.canonicalPath.replace(diskusage.toRegex(), app!!.pkg)
                moveIntoPackage(app, media, cacheDir, "Cache", ChildType.CACHE, blockSize)
            } catch (ignored: IOException) {
            }
        }
        for (a in apps) {
            val app = a as FileSystemPackage?
            try {
                val dir = codeCacheDir.canonicalPath.replace(diskusage.toRegex(), app!!.pkg)
                moveIntoPackage(app, media, dir, "CodeCache", ChildType.CACHE, blockSize)
            } catch (ignored: IOException) {
            }
        }
        for (a in apps) {
            val app = a as FileSystemPackage?
            try {
                val dir = externalCacheDir!!.canonicalPath.replace(diskusage.toRegex(), app!!.pkg)
                moveIntoPackage(app, media, dir, "ExternalCache", ChildType.CACHE, blockSize)
            } catch (ignored: IOException) {
            }
        }
        for (a in apps) {
            val app = a as FileSystemPackage?
            try {
                val dir = dataDir.canonicalPath.replace(diskusage.toRegex(), app!!.pkg)
                moveIntoPackage(app, media, dir, "Data", ChildType.DATA, blockSize)
            } catch (ignored: IOException) {
            }
        }
        for (a in apps) {
            val app = a as FileSystemPackage?
            try {
                val dir = filesDir.canonicalPath.replace(diskusage.toRegex(), app!!.pkg)
                moveIntoPackage(app, media, dir, "InternalFiles", ChildType.DATA, blockSize)
            } catch (ignored: IOException) {
            }
        }
        for (a in apps) {
            val app = a as FileSystemPackage?
            try {
                val dir = getExternalFilesDir(null)!!.canonicalPath.replace(
                    diskusage.toRegex(),
                    app!!.pkg
                )
                moveIntoPackage(app, media, dir, "Files", ChildType.DATA, blockSize)
            } catch (ignored: IOException) {
            }
        }
        for (a in apps) {
            val app = a as FileSystemPackage?
            try {
                for (mediaDir in externalMediaDirs) {
                    val dir = mediaDir.canonicalPath.replace(diskusage.toRegex(), app!!.pkg)
                    moveIntoPackage(app, media, dir, "MediaFiles", ChildType.DATA, blockSize)
                }
            } catch (ignored: IOException) {
            }
        }
        for (a in apps) {
            val app = a as FileSystemPackage?
            try {
                for (mediaDir in obbDirs) {
                    val dir = mediaDir.canonicalPath.replace(diskusage.toRegex(), app!!.pkg)
                    moveIntoPackage(app, media, dir, "Obb", ChildType.CODE, blockSize)
                }
            } catch (ignored: IOException) {
            }
        }
        for (a in apps) {
            val app = a as FileSystemPackage?
            app!!.applyFilter(blockSize)
        }
        Arrays.sort(apps, FileSystemEntry.COMPARE)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        this.menu.onPrepareOptionsMenu(menu)
        return true
    }

    override var key: String = ""

    fun searchRequest() {
        menu.searchRequest()
    }

    companion object {
        const val RESULT_DELETE_CONFIRMED = 10
        const val RESULT_DELETE_CANCELED = 11
        const val STATE_KEY = "state"
        const val KEY_KEY = "key"
        const val DELETE_PATH_KEY = "path"
        const val DELETE_ABSOLUTE_PATH_KEY = "absolute_path"
    } */
}

fun a() {}

@Composable
fun LinearDeterminateIndicator(onDismissRequest : () -> Unit) {
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    var currentFile by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope() // Create a coroutine scope

    LaunchedEffect(scope) {
        scope.launch {
            loadProgress { progress, string ->
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

/** Iterate the progress value */
suspend fun loadProgress(updateProgress: (Float, String) -> Unit) {
    for (i in 1..100) {
        updateProgress(i.toFloat() / 100, if (i % 3 == 0) "Fizz" else if (i % 5 == 0) "Buzz" else "FizzBuzz")
        delay(100)
    }
}

