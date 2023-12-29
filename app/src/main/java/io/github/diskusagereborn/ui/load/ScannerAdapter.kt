package io.github.diskusagereborn.ui.load

interface ScannerAdapter {
    suspend fun sourceUpdate(position : Long, name : String)

    fun getCurrentPos() : Long
}