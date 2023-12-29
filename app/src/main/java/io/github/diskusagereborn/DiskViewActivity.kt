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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot
import io.github.diskusagereborn.ui.diskview.FileRectangle
import io.github.diskusagereborn.ui.diskview.FileRectangleInitializer
import io.github.diskusagereborn.ui.theme.DiskUsageTheme
import io.github.diskusagereborn.utils.ObjectWrapperForBinder
import kotlin.math.roundToInt


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
    var selectedRectangle : FileRectangle by remember {
        mutableStateOf(rectangles.find { x -> x.depthLevel == 0 }!!)
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("${selectedRectangle.name} / ${selectedRectangle.displaySize}")
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x * scale,
                    translationY = offset.y * scale
                )
                .transformable(state = state)
                .fillMaxSize()
                .onSizeChanged {
                    size = it
                }
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            for (rect in rectangles) {
                                if (rect.rectangle.contains(tapOffset)) {
                                    selectedRectangle = rect
                                    val actualScale = size.height / rect.height
                                    val actualOffset = Offset(
                                        -rect.offsetX,
                                        -rect.offsetY
                                    ) / actualScale
                                    val consumedSize = Offset(
                                        rect.width * (actualScale - 1F),
                                        rect.height * (actualScale - 1F),
                                    ) / actualScale
                                    scale = actualScale
                                    offset = actualOffset
                                    offset += consumedSize
                                    break
                                }
                            }
                        }
                    )
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (rectangle in rectangles) {
                    drawRect(
                        topLeft = Offset(rectangle.offsetX, rectangle.offsetY),
                        color = Color.Black,
                        size = Size(rectangle.width, rectangle.height),
                        style = Stroke(
                            width = 1F.coerceAtMost(rectangle.height * 0.05F).dp.toPx()
                        ),
                    )
                    drawRect(
                        topLeft = Offset(rectangle.offsetX, rectangle.offsetY),
                        color = colorsByDepth[if (rectangle.depthLevel > 5) 5 else rectangle.depthLevel],
                        size = Size(rectangle.width, rectangle.height),
                    )
                    val textLayoutResult: TextLayoutResult =
                        textMeasurer.measure(
                            text = AnnotatedString("${rectangle.name}\n${rectangle.displaySize}"),
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = (if (rectangle.calculatedFontSize == null)
                                    24F / scale else rectangle.calculatedFontSize!!.value)
                                    .coerceAtLeast(24F / scale)
                                    .toSp(),
                                fontWeight = FontWeight.Bold
                            ),
                            constraints = Constraints.fixed(rectangle.width.roundToInt(), rectangle.height.roundToInt())
                        )
                    if (!textLayoutResult.hasVisualOverflow) {
                        if (rectangle.calculatedFontSize == null)
                            rectangle.calculatedFontSize = (24F / scale).toSp()
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(rectangle.offsetX, rectangle.offsetY)
                        )
                    }
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
                    size = Size(300F.dp.toPx(), 300F)
                )
                drawRect(
                    topLeft = Offset(400F, 100F),
                    brush = Brush.linearGradient(listOf(Color.LightGray, Color.DarkGray)),
                    size = Size(300F, 300F)
                )
            }
        }
    }
}
