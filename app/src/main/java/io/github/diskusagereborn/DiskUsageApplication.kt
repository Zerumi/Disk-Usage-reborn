/*
 * DiskUsage Reborn - Rewritten on modern stack version of DiskUsage
 * Copyright (C) 2023-2024 Zerumi
 *
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2008-2011 Ivan Volosyuk
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
package io.github.diskusagereborn

import android.app.Application

class DiskUsageApplication : Application() {
    companion object {

        @Volatile
        private var instance : DiskUsageApplication? = null

        fun getInstance() =
            instance ?:
            throw IllegalStateException("DiskUsage application is not created!")

        const val RESULT_DELETE_CONFIRMED = 10
        const val RESULT_DELETE_CANCELED = 11
        const val STATE_KEY = "state"
        const val KEY_KEY = "key"
        const val DELETE_PATH_KEY = "path"
        const val DELETE_ABSOLUTE_PATH_KEY = "absolute_path"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
