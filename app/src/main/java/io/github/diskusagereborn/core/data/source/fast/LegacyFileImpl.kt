package io.github.diskusagereborn.core.data.source.fast

import io.github.diskusagereborn.core.data.source.LegacyFile
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.IOException

class LegacyFileImpl private constructor(private val file: File) : LegacyFile {

    override val name: String?
        get() = file.name

    @get:Throws(IOException::class)
    override val canonicalPath: String?
        get() = file.canonicalPath


    override val isLink: Boolean
        get() = try {
            file.canonicalPath != file.path
        } catch (e: Throwable) {
            true
        }

    override val isFile: Boolean
        get() = file.isFile

    override fun length(): Long {
        return file.length()
    }

    override fun listFiles(): Array<LegacyFile?> {
        val children = file.listFiles()
        val res = arrayOfNulls<LegacyFile>(children?.size ?: 0)
        if (children != null) {
            for (i in children.indices) {
                res[i] = LegacyFileImpl(children[i])
            }
        }
        return res
    }

    override fun list(): Array<String?>? {
        return file.list()
    }

    override fun getChild(childName: String?): LegacyFile {
        return LegacyFileImpl(File(file, childName))
    }

    companion object {
        @JvmStatic
        @Contract("_ -> new")
        fun createRoot(root: String?): LegacyFile {
            return LegacyFileImpl(File(root!!))
        }
    }
}
