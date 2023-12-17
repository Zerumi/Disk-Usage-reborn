package io.github.diskusagereborn.core.data.source.fast

import android.os.StatFs
import io.github.diskusagereborn.core.data.source.StatFsSource

class StatFsSourceImpl(path: String?) : StatFsSource {
    private val statFs: StatFs

    init {
        statFs = StatFs(path)
    }

    @Deprecated("")
    override val availableBlocks: Int
        get() = statFs.availableBlocks

    override val availableBlocksLong: Long
        get() = statFs.availableBlocksLong

    override val availableBytes: Long
        get() = statFs.availableBytes

    @Deprecated("")
    override val blockCount: Int
        get() = statFs.blockCount

    override val blockCountLong: Long
        get() = statFs.blockCountLong

    @Deprecated("")
    override val blockSize: Int
        get() = statFs.blockSize

    override val blockSizeLong: Long
        get() = statFs.blockSizeLong

    override val freeBytes: Long
        get() = statFs.freeBytes

    @Deprecated("")
    override val freeBlocks: Int
        get() = statFs.freeBlocks

    override val freeBlocksLong: Long
        get() = statFs.freeBlocksLong

    override val totalBytes: Long
        get() = statFs.totalBytes
}
