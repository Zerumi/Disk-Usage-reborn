/*
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2008 Ivan Volosyuk
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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.diskusagereborn.core.fs.entity.FileSystemEntry
import io.github.diskusagereborn.ui.theme.DiskUsageTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class LoadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileSystemEntry.setupStrings(this)
        setContent {
            DiskUsageTheme {
                LinearDeterminateIndicator(::loadProgress) { finish() }
            }
        }
    }

    /** Iterate the progress value */
    private suspend fun loadProgress(updateProgress: (Float, String) -> Unit) {
        for (i in 1..100) {
            updateProgress(i.toFloat() / 100, if (i % 3 == 0) "Fizz" else if (i % 5 == 0) "Buzz" else "FizzBuzz")
            delay(100)
        }
    }

}
@Composable
fun LinearDeterminateIndicator(
    progressBarUpdater : suspend ((Float, String) -> Unit) -> Unit,
    onDismissRequest : () -> Unit) {
    var currentProgress by remember { mutableStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    var currentFile by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope() // Create a coroutine scope

    LaunchedEffect(scope) {
        scope.launch {
            progressBarUpdater { progress, string ->
                currentProgress = progress
                currentFile = string
            }
            loading = false // Reset loading when the coroutine finishes
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Scanning directories...",
                    modifier = Modifier
                        .padding(4.dp)
                )
                LinearProgressIndicator(
                    progress = currentProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Current: ",
                        modifier = Modifier
                            .padding(3.dp)
                    )
                    Text(
                        text = currentFile,
                        modifier = Modifier
                            .padding(3.dp)
                    )
                }
            }
        }
    }
}

