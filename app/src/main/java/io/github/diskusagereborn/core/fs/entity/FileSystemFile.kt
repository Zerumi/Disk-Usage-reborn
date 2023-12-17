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
