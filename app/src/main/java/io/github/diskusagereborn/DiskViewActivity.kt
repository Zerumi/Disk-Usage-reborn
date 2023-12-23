package io.github.diskusagereborn

import android.app.Activity
import android.content.ContentValues.TAG
import android.graphics.Insets
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot
import io.github.diskusagereborn.ui.diskview.FileRectangle
import io.github.diskusagereborn.ui.diskview.FileRectangleInitializer
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
                UsageView(
                    FileRectangleInitializer(
                        getScreenHeight(this) - 4,
                        objReceived!!)
                        .getRectangles())
            }
        }
    }

    private fun getScreenHeight(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }
}


val colorsByDepth : Array<Color> =
    arrayOf(
        Color.Cyan,
        Color.Green,
        Color.Magenta,
        Color.Yellow,
        Color.DarkGray,
        Color.LightGray
    )

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
fun UsageView(rectangles : Array<FileRectangle>) {
    val textMeasurer = rememberTextMeasurer()
    // set up all transformation states
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("DiskUsage Reborn") // todo useful space for information
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                // apply other transformations like rotation and zoom
                // on the pizza slice emoji
                .graphicsLayer(
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y * scale
                )
                // add transformable to listen to multitouch transformation events
                // after offset
                .transformable(state = state)
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (rectangle in rectangles) {
                    drawRect(
                        topLeft = Offset(rectangle.offsetX, rectangle.offsetY),
                        color = Color.Black,
                        size = Size(rectangle.width, rectangle.height),
                        style = Stroke(width = 2.dp.toPx()), // todo outside border, to prevent small rectangles hiding
                    )
                    drawRect(
                        topLeft = Offset(rectangle.offsetX, rectangle.offsetY),
                        color = colorsByDepth[if (rectangle.depthLevel > 5) 5 else rectangle.depthLevel],
                        size = Size(rectangle.width, rectangle.height),
                    )
                    /* drawText(
                        textMeasurer = textMeasurer,
                        text = rectangle.name,
                        //topLeft = Offset(rectangle.offsetX, rectangle.offsetY)
                    ) */
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun Prev() {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("DiskUsage Reborn")
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                // apply other transformations like rotation and zoom
                // on the pizza slice emoji
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                // add transformable to listen to multitouch transformation events
                // after offset
                .transformable(state = state)
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    topLeft = Offset(0F, 0F),
                    brush = Brush.linearGradient(listOf(Color.LightGray, Color.DarkGray)),
                    size = Size(300F, 300F)
                )
                drawRect(
                    topLeft = Offset(400F, 100F),
                    brush = Brush.linearGradient(listOf(Color.LightGray, Color.DarkGray)),
                    size = Size(300F, 300F)
                )
            } // todo click on rectangle
            // fixme https://stackoverflow.com/questions/68363029/how-to-add-click-events-to-canvas-in-jetpack-compose
        }
    }
}
