package io.github.diskusagereborn.core.data.source

interface StatFsSource {
    @get:Deprecated("")
    val availableBlocks: Int

    val availableBlocksLong: Long

    val availableBytes: Long

    @get:Deprecated("")
    val blockCount: Int

    val blockCountLong: Long

    @get:Deprecated("")
    val blockSize: Int

    val blockSizeLong: Long

    val freeBytes: Long

    @get:Deprecated("")
    val freeBlocks: Int

    val freeBlocksLong: Long

    val totalBytes: Long
}