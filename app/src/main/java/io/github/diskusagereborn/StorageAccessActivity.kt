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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.diskusagereborn.ui.theme.DiskUsageTheme

class StorageAccessActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasAccess : Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                checkAccessR()
            else
                checkAccessM()

        if (hasAccess) {
            goToLoad()
        }

        setContent {
            DiskUsageTheme {
                RequireAccessDialog(title = "Access required",
                    message =  "Access Required. Please, confirm to proceed setting necessary permissions, after them restart the app",
                    onConfirm = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) requestAccessR()
                        else requestAccessM()
                    }) { finish() }
            }
        }
    }

    private fun goToLoad() {
        val loadActivity =
            Intent(this@StorageAccessActivity, LoadActivity::class.java)
        loadActivity.putExtra(DiskUsageApplication.KEY_KEY,
            this.intent.getStringExtra(DiskUsageApplication.KEY_KEY))
        startActivity(loadActivity)
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestAccessR() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }

    private fun requestAccessM() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkAccessR() : Boolean = Environment.isExternalStorageManager()

    private fun checkAccessM() : Boolean =
        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
}

@Composable
fun RequireAccessDialog(
    title : String,
    message : String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit) {
    AlertDialog(
        title = {
                Text(text = title)
        },
        text = {
               Text(text = message)
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                }
            ) {
                Text("Confirm")
            }
        }
    )
}