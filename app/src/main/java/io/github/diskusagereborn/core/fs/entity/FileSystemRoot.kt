package io.github.diskusagereborn.core.fs.entity

import org.jetbrains.annotations.Contract

class FileSystemRoot private constructor(
    name: String?,
    val rootPath: String,
    override val isDeletable: Boolean
) : FileSystemEntry(null, name ?: "") {

    override fun create(): FileSystemEntry {
        return FileSystemRoot(name, rootPath, isDeletable)
    }

    override fun filter(pattern: CharSequence?, blockSize: Long): FileSystemEntry? {
        // don't match name
        return filterChildren(pattern, blockSize)
    }

    fun getByAbsolutePath(path: String): FileSystemEntry? {
        val rootPathWithSlash = withSlash(
            rootPath
        )
        val pathWithSlash = withSlash(path)
        if (pathWithSlash == rootPathWithSlash) {
            return getEntryByName(path, true)
        }
        if (pathWithSlash.startsWith(rootPathWithSlash)) {
            return getEntryByName(path.substring(rootPathWithSlash.length), true)
        }
        for (s in children!!) {
            if (s is FileSystemRoot) {
                val e: FileSystemEntry? = s.getByAbsolutePath(path)
                if (e != null) return e
            }
        }
        return null
    }

    companion object {
        @JvmStatic
        @Contract("_, _, _ -> new")
        fun makeNode(name: String?, rootPath: String, deletable: Boolean): FileSystemRoot {
            return FileSystemRoot(name, rootPath, deletable)
        }

        fun withSlash(sourcePath: String): String {
            var path = sourcePath
            if (path.isNotEmpty() && path[path.length - 1] != '/') path += '/'
            return path
        }
    }
}
