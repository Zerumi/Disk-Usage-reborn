/*
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2008-2011 Ivan Volosyuk
 *
 * DiskUsage Reborn - Rewritten on modern stack version of DiskUsage
 * Copyright (C) 2023-2024 Zerumi
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
package io.github.diskusagereborn.utils

import android.util.Log
import java.util.Locale

class Logger(private val tag: String) {
    companion object {
        @JvmStatic
        val LOGGER: Logger = Logger("DiskUsage")
    }

    private fun isLoggable(tag: String, level: Int): Boolean {
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