package io.github.diskusagereborn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
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

        setContent {
            DiskUsageTheme {
                RequireAccessDialog(title = "Access require",
                    message = if (hasAccess) "Access Granted" else "Access Required!!",
                    onConfirm = {}) {}
            }
        }
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