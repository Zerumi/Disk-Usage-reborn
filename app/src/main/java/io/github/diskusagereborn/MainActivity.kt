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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.diskusagereborn.core.fs.mount.MountPoint
import io.github.diskusagereborn.ui.theme.DiskUsageTheme
import io.github.diskusagereborn.utils.AppHelper

class MainActivity : ComponentActivity() {

    private lateinit var mountList: List<MountPoint>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mountList = MountPoint.getMountPoints(AppHelper.appContext)
        setContent {
            DiskUsageTheme {
                StartDialog(
                    contents = generateChooseList(),
                    onSelect = this::onSelect) {}
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        DiskUsageTheme {
            StartDialog(contents = arrayOf("1234", "12345"), onSelect = this::onSelect) {}
        }
    }

    private fun generateChooseList() : Array<String> {
        val mountTitles : List<String> = mountList.mapNotNull { mnt -> mnt.title }
        return mountTitles.toTypedArray()
    }

    private fun onSelect(optionIndex: Int) {
        val checkAccessActivity
            = Intent(this@MainActivity, StorageAccessActivity::class.java)
        checkAccessActivity.putExtra(DiskUsageApplication.KEY_KEY, mountList[optionIndex].key)
        startActivity(checkAccessActivity)
        finish()
    }
}
@Composable
fun StartDialog(contents : Array<String>,
                onSelect : (Int) -> Unit,
                onDismissRequest : () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Choose disk to examine.",
                    modifier = Modifier
                        .padding(16.dp, 16.dp, 16.dp, 0.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 0.dp, 16.dp, 0.dp)
                ) {
                    for (i in contents.indices) {
                        TextButton(
                            onClick = { onSelect(i) },
                        ) {
                            Text(contents[i])
                        }
                    }
                }
            }
        }
    }
}
