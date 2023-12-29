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

import android.content.Context
import android.content.res.Configuration
import io.github.diskusagereborn.BuildConfig

// Adapted from https://gist.github.com/hendrawd/01f215fd332d84793e600e7f82fc154b
object DeviceInfo {
    fun get(context: Context) =
        buildString {
            appendLine("App Package Name: ${BuildConfig.APPLICATION_ID}")
            appendLine("App Version Name: ${BuildConfig.VERSION_NAME}")
            appendLine("App Version Code: ${BuildConfig.VERSION_CODE}")
            appendLine("OS Name: ${android.os.Build.DISPLAY}")
            appendLine("OS Version: ${System.getProperty("os.version")} (${android.os.Build.VERSION.INCREMENTAL})")
            appendLine("OS API Level: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("Device: ${android.os.Build.DEVICE}")
            appendLine("Model (product): ${android.os.Build.MODEL} (${android.os.Build.PRODUCT})")
            appendLine("Manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("Tags: ${android.os.Build.TAGS}")
            val metrics = context.resources.displayMetrics
            appendLine("Screen Size: ${metrics.widthPixels} x ${metrics.heightPixels}")
            appendLine("Screen Density: ${metrics.density}")
            appendLine(
                "Screen orientation: ${
                    when (context.resources.configuration.orientation) {
                        Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                        Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                        Configuration.ORIENTATION_UNDEFINED -> "Undefined"
                        else -> "Unknown"
                    }
                }"
            )
        }
}
