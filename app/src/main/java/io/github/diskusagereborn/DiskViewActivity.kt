package io.github.diskusagereborn

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot
import io.github.diskusagereborn.ui.theme.DiskUsageTheme
import io.github.diskusagereborn.utils.ObjectWrapperForBinder

class DiskViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val objReceived: FileSystemSuperRoot? =
            (intent.extras!!.getBinder("object_value") as ObjectWrapperForBinder?)?.data

        if (objReceived == null) finish()

        Log.d(TAG, "received object=$objReceived")

        setContent {
            DiskUsageTheme {
                UsageView()
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun UsageView() {
    Text(text = "DiskViewActivity")
}