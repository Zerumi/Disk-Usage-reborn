package io.github.diskusagereborn.core.data.source

interface PortableFile {
    val isExternalStorageEmulated: Boolean

    val isExternalStorageRemovable: Boolean

    /** Retries with getAbsolutePath() on IOException  */
    val canonicalPath: String?

    val absolutePath: String?

    val totalSpace: Long
}
