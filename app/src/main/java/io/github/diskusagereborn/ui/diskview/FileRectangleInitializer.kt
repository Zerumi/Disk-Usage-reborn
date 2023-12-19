package io.github.diskusagereborn.ui.diskview

import io.github.diskusagereborn.core.fs.entity.FileSystemEntry
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot


class FileRectangleInitializer(val maxHeight : Int, val root : FileSystemSuperRoot) {

    companion object {
        private const val WIDTH = 300F
    }

    private val resultArray : ArrayList<FileRectangle> = arrayListOf()
    private val currentOffsetsX : MutableMap<Int, Float> = mutableMapOf()

    fun getRectangles() : Array<FileRectangle> {

        iterateChild(root, -1)

        return resultArray.toTypedArray()
    }

    private fun iterateChild(child : FileSystemEntry, currentDepth : Int) {
        if (!currentOffsetsX.containsKey(currentDepth)) currentOffsetsX[currentDepth] = 0F
        if (child.children != null) {
            for (iteratingChild: FileSystemEntry in child.children!!.filterNotNull()) {
                iterateChild(iteratingChild, currentDepth + 1)
            }
        }

        if (child == root) return

        val calculatedName = child.name
        val calculatedDisplaySize = child.sizeString()
        val calculatedHeight = child.encodedSize.toFloat() / root.encodedSize * maxHeight
        val calculatedWidth = WIDTH
        val calculatedOffsetX = currentOffsetsX[currentDepth]
        val calculatedOffsetY = WIDTH * currentDepth

        resultArray.add(FileRectangle(
            calculatedName,
            calculatedDisplaySize,
            calculatedHeight,
            calculatedWidth,
            // revert axis, that's important
            offsetY = calculatedOffsetX!!,
            offsetX = calculatedOffsetY,
            depthLevel =  currentDepth))

        currentOffsetsX[currentDepth] = currentOffsetsX[currentDepth]!! + calculatedHeight
    }

}