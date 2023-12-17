package io.github.diskusagereborn.core.data.source

import java.io.IOException

interface LegacyFile {
    val name: String?

    @get:Throws(IOException::class)
    val canonicalPath: String?
    val isLink: Boolean
    val isFile: Boolean
    fun length(): Long
    fun listFiles(): Array<LegacyFile?>?
    fun list(): Array<String?>?
    fun getChild(childName: String?): LegacyFile?
}