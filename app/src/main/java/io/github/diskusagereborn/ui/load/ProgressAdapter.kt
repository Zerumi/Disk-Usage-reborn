package io.github.diskusagereborn.ui.load

import io.github.diskusagereborn.core.fs.FileSystemStats


class ProgressAdapter (
    val rootFsStat : FileSystemStats,
    val uiProgressUpdater : suspend (Float, String) -> Unit) : ScannerAdapter {

        private var currentPos : Long = 0
     override suspend fun sourceUpdate(position: Long, name: String) {
         uiProgressUpdater(position.toFloat() / rootFsStat.busyBlocks, name)
         currentPos = position
     }

    override fun getCurrentPos(): Long {
        return currentPos
    }
}