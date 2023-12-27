package io.github.diskusagereborn.ui.diskview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.TextUnit

class FileRectangle(val name : String, val displaySize : String,
                    val height : Float, val width: Float,
                    val offsetX : Float, val offsetY : Float,
                    val depthLevel : Int) {

    val rectangle : Rect = Rect(offset = Offset(offsetX, offsetY), size=Size(width, height))
    var calculatedFontSize : TextUnit? = null
}