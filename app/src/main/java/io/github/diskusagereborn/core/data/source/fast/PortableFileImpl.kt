package io.github.diskusagereborn.core.data.source.fast

import android.os.Environment
import io.github.diskusagereborn.core.data.source.PortableFile
import io.github.diskusagereborn.utils.PathHelper.getExternalAppFilesPaths
import java.io.File

class PortableFileImpl private constructor(private val file: File) : PortableFile {

    override val isExternalStorageEmulated: Boolean
        get() = try {
            Environment.isExternalStorageEmulated(file)
        } catch (e: Exception) {
            false
        }

    override val isExternalStorageRemovable: Boolean
        get() = try {
            Environment.isExternalStorageRemovable(file)
        } catch (e: Exception) {
            false
        }

    override val canonicalPath: String?
        get() = try {
            file.canonicalPath
        } catch (e: Exception) {
            file.absolutePath
        }

    override val absolutePath: String?
        get() = file.absolutePath

    override val totalSpace: Long
        get() = file.totalSpace

    override fun equals(other: Any?): Boolean {
        if (other !is PortableFile) {
            return false
        }
        return other.absolutePath == absolutePath
    }

    override fun hashCode(): Int {
        return absolutePath.hashCode()
    }

    companion object {
        private fun make(file: File?): PortableFileImpl? {
            return file?.let { PortableFileImpl(it) }
        }

        @JvmStatic
        val externalAppFilesDirs: Array<PortableFile?>
            get() {
                val externalAppFilesPaths: Array<out File> = getExternalAppFilesPaths()
                val result = arrayOfNulls<PortableFile>(externalAppFilesPaths.size)
                var i = 0
                for (dir in externalAppFilesPaths) {
                    result[i++] = make(dir)
                }
                return result
            }
    }
}