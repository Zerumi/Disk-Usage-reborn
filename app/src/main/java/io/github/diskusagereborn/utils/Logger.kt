package io.github.diskusagereborn.utils

import android.util.Log
import java.util.Locale

class Logger(private val tag: String) {
    companion object {
        @JvmStatic
        val LOGGER: Logger = Logger("DiskUsage")
    }

    fun isLoggable(tag: String, level: Int): Boolean {
        return true
    }

    fun v(msg: String) {
        if (isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, msg)
        }
    }

    fun v(fmt: String, vararg args: Any?) {
        if (isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, String.format(Locale.ENGLISH, fmt, *args))
        }
    }

    fun v(msg: String, tr: Throwable) {
        if (isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, msg, tr)
        }
    }

    fun d(msg: String) {
        if (isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, msg)
        }
    }

    fun d(fmt: String, vararg args: Any?) {
        if (isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, String.format(Locale.ENGLISH, fmt, *args))
        }
    }

    fun d(msg: String, tr: Throwable) {
        if (isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, msg, tr)
        }
    }

    fun i(msg: String) {
        if (isLoggable(tag, Log.INFO)) {
            Log.i(tag, msg)
        }
    }

    fun i(fmt: String, vararg args: Any?) {
        if (isLoggable(tag, Log.INFO)) {
            Log.i(tag, String.format(Locale.ENGLISH, fmt, *args))
        }
    }

    fun i(msg: String, tr: Throwable) {
        if (isLoggable(tag, Log.INFO)) {
            Log.i(tag, msg, tr)
        }
    }

    fun w(msg: String) {
        if (isLoggable(tag, Log.WARN)) {
            Log.w(tag, msg)
        }
    }

    fun w(fmt: String, vararg args: Any?) {
        if (isLoggable(tag, Log.WARN)) {
            Log.w(tag, String.format(Locale.ENGLISH, fmt, *args))
        }
    }

    fun w(tr: Throwable, fmt: String, vararg args: Any?) {
        if (isLoggable(tag, Log.WARN)) {
            Log.w(tag, String.format(Locale.ENGLISH, fmt, *args), tr)
        }
    }

    fun w(msg: String, tr: Throwable) {
        if (isLoggable(tag, Log.WARN)) {
            Log.w(tag, msg, tr)
        }
    }

    fun e(msg: String) {
        if (isLoggable(tag, Log.ERROR)) {
            Log.e(tag, msg)
        }
    }

    fun e(fmt: String, vararg args: Any?) {
        if (isLoggable(tag, Log.ERROR)) {
            Log.e(tag, String.format(Locale.ENGLISH, fmt, *args))
        }
    }

    fun e(tr: Throwable, fmt: String, vararg args: Any?) {
        if (isLoggable(tag, Log.ERROR)) {
            Log.e(tag, String.format(Locale.ENGLISH, fmt, *args), tr)
        }
    }

    fun e(msg: String, tr: Throwable) {
        if (isLoggable(tag, Log.ERROR)) {
            Log.e(tag, msg, tr)
        }
    }
}