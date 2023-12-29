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
package io.github.diskusagereborn.ui.diskview

import io.github.diskusagereborn.core.fs.entity.FileSystemEntry
import io.github.diskusagereborn.core.fs.entity.FileSystemSuperRoot


class FileRectangleInitializer(private val maxHeight : Int, val root : FileSystemSuperRoot) {

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