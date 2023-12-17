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
