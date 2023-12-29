/*
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2008 Ivan Volosyuk
 *
 * DiskUsage Reborn - Rewritten on modern stack version of DiskUsage
 * Copyright (C) 2023 Zerumi
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
package io.github.diskusagereborn.core.fs.entity

import org.jetbrains.annotations.Contract

class FileSystemFile private constructor(parent: FileSystemEntry?, name: String) :
    FileSystemEntry(parent, name) {
    // --Commented out by Inspection START (16.12.2023, 01:29):
    //  @Override
    //  public boolean isDeletable() {
    //    return true;
    //  }
    // --Commented out by Inspection STOP (16.12.2023, 01:29)
    override fun create(): FileSystemEntry {
        return FileSystemFile(null, name)
    }

    companion object {
        @Contract("_, _ -> new")
        fun makeNode(
            parent: FileSystemEntry?, name: String
        ): FileSystemEntry {
            return FileSystemFile(parent, name)
        }
    }
}
