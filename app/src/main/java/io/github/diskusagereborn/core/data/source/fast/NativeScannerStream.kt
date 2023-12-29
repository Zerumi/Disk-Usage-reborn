/*
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2008 Ivan Volosyuk
 *
 * DiskUsage Reborn - Rewritten on modern stack version of DiskUsage
 * Copyright (C) 2023 Zerumi
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
package io.github.diskusagereborn.core.data.source.fast

import io.github.diskusagereborn.utils.AppHelper.appContext
import io.github.diskusagereborn.utils.DeviceHelper.isDeviceRooted
import org.jetbrains.annotations.Contract
import java.io.IOException
import java.io.InputStream

class NativeScannerStream(private val `is`: InputStream, private val process: Process?) :
    InputStream() {
    @Throws(IOException::class)
    override fun read(): Int {
        return `is`.read()
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray): Int {
        return `is`.read(buffer)
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, byteOffset: Int, byteCount: Int): Int {
        return `is`.read(buffer, byteOffset, byteCount)
    }

    @Throws(IOException::class)
    override fun close() {
        `is`.close()
        try {
            process!!.waitFor()
        } catch (e: InterruptedException) {
            throw IOException(e.message)
        }
    }

    object Factory {
        // private static final boolean remove = true;
        @Contract("_, _ -> new")
        @Throws(IOException::class, InterruptedException::class)
        fun create(path: String, rootRequired: Boolean): NativeScannerStream {
            return runScanner(path, rootRequired)
        }

        @Contract("_, _ -> new")
        @Throws(IOException::class, InterruptedException::class)
        private fun runScanner(root: String, rootRequired: Boolean): NativeScannerStream {
            val binaryName = "libscan.so"
            var process: Process? = null
            if (!(rootRequired && isDeviceRooted())) {
                process = Runtime.getRuntime().exec(
                    arrayOf(
                        getScanBinaryPath(binaryName), root
                    )
                )
            } else {
                var e: IOException? = null
                for (su in arrayOf<String>("su", "/system/bin/su", "/system/xbin/su")) {
                    try {
                        process = Runtime.getRuntime().exec(arrayOf(su))
                        break
                    } catch (newe: IOException) {
                        e = newe
                    }
                }
                if (process == null) {
                    throw e!!
                }
                val os = process.outputStream
                os.write((getScanBinaryPath(binaryName) + " " + root).toByteArray(charset("UTF-8")))
                os.flush()
                os.close()
            }
            val `is` = process!!.inputStream
            return NativeScannerStream(`is`, process)
        }

        private fun getScanBinaryPath(binaryName: String): String {
            return (appContext.applicationInfo.nativeLibraryDir
                    + "/" + binaryName)
        }
    }
}
